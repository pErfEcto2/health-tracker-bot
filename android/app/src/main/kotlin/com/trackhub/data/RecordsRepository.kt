package com.trackhub.data

import com.trackhub.api.ApiService
import com.trackhub.api.EncryptedRecord
import com.trackhub.api.RecordCreateRequest
import com.trackhub.api.RecordUpdateRequest
import com.trackhub.crypto.Crypto
import com.trackhub.session.DekManager
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Generic CRUD over the encrypted /records endpoint. Decryption happens here
 * so screens get already-decrypted payloads.
 *
 * Inline reified helpers exist for callers that want type-safe access. They
 * forward to non-inline implementations so the inline functions only touch
 * public surface (Kotlin restricts inline funcs from non-public members).
 */
class RecordsRepository(
    private val api: ApiService,
    private val dekManager: DekManager,
    private val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true },
) {

    data class Decrypted<T>(
        val id: String,
        val type: String,
        val recordDate: String,
        val payload: T,
        val createdAt: String,
        val updatedAt: String,
    )

    private fun dek(): ByteArray =
        dekManager.memoryDek() ?: error("DEK not unlocked")

    suspend inline fun <reified T> list(
        type: String? = null,
        from: String? = null,
        to: String? = null,
    ): List<Decrypted<T>> = listImpl(type, from, to, kotlinx.serialization.serializer())

    suspend inline fun <reified T> get(id: String): Decrypted<T> =
        getImpl(id, kotlinx.serialization.serializer())

    suspend inline fun <reified T> create(type: String, recordDate: String, payload: T): Decrypted<T> =
        createImpl(type, recordDate, payload, kotlinx.serialization.serializer())

    suspend inline fun <reified T> update(id: String, payload: T, recordDate: String? = null): Decrypted<T> =
        updateImpl(id, payload, recordDate, kotlinx.serialization.serializer())

    suspend fun delete(id: String) { api.deleteRecord(id) }

    // ---- non-inline impls (can touch private members) ----

    suspend fun <T> listImpl(
        type: String?, from: String?, to: String?, ser: KSerializer<T>,
    ): List<Decrypted<T>> {
        val raw = api.listRecords(type, from, to)
        return raw.mapNotNull { tryDecrypt(it, ser) }
    }

    suspend fun <T> getImpl(id: String, ser: KSerializer<T>): Decrypted<T> {
        val raw = api.getRecord(id)
        return tryDecrypt(raw, ser) ?: error("decrypt failed for $id")
    }

    suspend fun <T> createImpl(
        type: String, recordDate: String, payload: T, ser: KSerializer<T>,
    ): Decrypted<T> {
        val ptBytes = json.encodeToString(ser, payload).toByteArray(Charsets.UTF_8)
        val blob = Crypto.encryptRecord(ptBytes, dek())
        val raw = api.createRecord(
            RecordCreateRequest(
                type = type, recordDate = recordDate,
                nonceHex = blob.nonceHex, ciphertextHex = blob.ciphertextHex,
            ),
        )
        return Decrypted(raw.id, raw.type, raw.recordDate, payload, raw.createdAt, raw.updatedAt)
    }

    suspend fun <T> updateImpl(
        id: String, payload: T, recordDate: String?, ser: KSerializer<T>,
    ): Decrypted<T> {
        val ptBytes = json.encodeToString(ser, payload).toByteArray(Charsets.UTF_8)
        val blob = Crypto.encryptRecord(ptBytes, dek())
        val raw = api.updateRecord(
            id,
            RecordUpdateRequest(
                recordDate = recordDate,
                nonceHex = blob.nonceHex, ciphertextHex = blob.ciphertextHex,
            ),
        )
        return Decrypted(raw.id, raw.type, raw.recordDate, payload, raw.createdAt, raw.updatedAt)
    }

    private fun <T> tryDecrypt(r: EncryptedRecord, ser: KSerializer<T>): Decrypted<T>? = try {
        val pt = Crypto.decryptRecord(
            Crypto.EncryptedBlob(r.nonceHex, r.ciphertextHex),
            dek(),
        )
        val payload = json.decodeFromString(ser, String(pt, Charsets.UTF_8))
        Decrypted(r.id, r.type, r.recordDate, payload, r.createdAt, r.updatedAt)
    } catch (_: Exception) { null }
}
