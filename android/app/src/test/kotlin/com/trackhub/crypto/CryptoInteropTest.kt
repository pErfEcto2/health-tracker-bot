package com.trackhub.crypto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression tests vs fixtures generated from the JS web client.
 * Run `node android/scripts/gen-fixtures.mjs` to regenerate.
 */
class CryptoInteropTest {

    private val fixtures: JsonObject by lazy {
        val raw = javaClass.classLoader!!.getResourceAsStream("crypto-fixtures.json")!!
            .bufferedReader().readText()
        Json.parseToJsonElement(raw).jsonObject
    }

    private fun obj(name: String): JsonObject = fixtures[name]!!.jsonObject
    private fun str(o: JsonObject, key: String) = o[key]!!.jsonPrimitive.content

    @Test
    fun pbkdf2_matches_js() {
        val o = obj("pbkdf2")
        val out = Crypto.pbkdf2(
            password = str(o, "password"),
            salt = Crypto.fromHex(str(o, "salt_hex")),
            iterations = o["iterations"]!!.jsonPrimitive.content.toInt(),
            length = o["len"]!!.jsonPrimitive.content.toInt(),
        )
        assertEquals(str(o, "expected_hex"), Crypto.toHex(out))
    }

    @Test
    fun hkdf_auth() {
        val o = obj("hkdf_auth")
        val out = Crypto.hkdf(Crypto.fromHex(str(o, "ikm_hex")), str(o, "info"), o["len"]!!.jsonPrimitive.content.toInt())
        assertEquals(str(o, "expected_hex"), Crypto.toHex(out))
    }

    @Test
    fun hkdf_enc() {
        val o = obj("hkdf_enc")
        val out = Crypto.hkdf(Crypto.fromHex(str(o, "ikm_hex")), str(o, "info"), o["len"]!!.jsonPrimitive.content.toInt())
        assertEquals(str(o, "expected_hex"), Crypto.toHex(out))
    }

    @Test
    fun hkdf_recovery() {
        val o = obj("hkdf_recovery")
        val out = Crypto.hkdf(Crypto.fromHex(str(o, "ikm_hex")), str(o, "info"), o["len"]!!.jsonPrimitive.content.toInt())
        assertEquals(str(o, "expected_hex"), Crypto.toHex(out))
    }

    @Test
    fun hkdf_recovery_auth() {
        val o = obj("hkdf_recovery_auth")
        val out = Crypto.hkdf(Crypto.fromHex(str(o, "ikm_hex")), str(o, "info"), o["len"]!!.jsonPrimitive.content.toInt())
        assertEquals(str(o, "expected_hex"), Crypto.toHex(out))
    }

    @Test
    fun aes_gcm_wrap_dek_matches_js() {
        val o = obj("aes_gcm_wrap_dek")
        val ct = Crypto.aesGcmEncrypt(
            plaintext = Crypto.fromHex(str(o, "plaintext_hex")),
            key = Crypto.fromHex(str(o, "key_hex")),
            nonce = Crypto.fromHex(str(o, "nonce_hex")),
        )
        assertEquals(str(o, "expected_ciphertext_hex"), Crypto.toHex(ct))
    }

    @Test
    fun aes_gcm_record_matches_js() {
        val o = obj("aes_gcm_record")
        val ct = Crypto.aesGcmEncrypt(
            plaintext = str(o, "plaintext_utf8").toByteArray(Charsets.UTF_8),
            key = Crypto.fromHex(str(o, "key_hex")),
            nonce = Crypto.fromHex(str(o, "nonce_hex")),
        )
        assertEquals(str(o, "expected_ciphertext_hex"), Crypto.toHex(ct))
    }

    @Test
    fun roundtrip_decrypts_js_ciphertext() {
        val o = obj("aes_gcm_record")
        val pt = Crypto.aesGcmDecrypt(
            ciphertextWithTag = Crypto.fromHex(str(o, "expected_ciphertext_hex")),
            key = Crypto.fromHex(str(o, "key_hex")),
            nonce = Crypto.fromHex(str(o, "nonce_hex")),
        )
        assertEquals(str(o, "plaintext_utf8"), String(pt, Charsets.UTF_8))
    }

    @Test
    fun derive_keys_end_to_end() {
        val pbk = obj("pbkdf2")
        val salt = Crypto.fromHex(str(pbk, "salt_hex"))
        val derived = Crypto.deriveKeysFromPassword(str(pbk, "password"), salt)
        assertEquals(str(obj("hkdf_auth"), "expected_hex"), derived.authKeyHex)
        assertEquals(str(obj("hkdf_enc"), "expected_hex"), Crypto.toHex(derived.kek))
    }

    @Test
    fun recovery_key_format_roundtrips() {
        val key = Crypto.fromHex("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff")
        val formatted = RecoveryKey.format(key)
        assertEquals("00112233 44556677 8899aabb ccddeeff 00112233 44556677 8899aabb ccddeeff", formatted)
        val parsed = RecoveryKey.parse(formatted)
        assertEquals(Crypto.toHex(key), Crypto.toHex(parsed))
    }
}
