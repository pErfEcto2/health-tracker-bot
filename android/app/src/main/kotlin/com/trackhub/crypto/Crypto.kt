package com.trackhub.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Mirror of src/app/static/src/crypto.ts (web client). Must produce byte-for-byte
 * identical output so encrypted records interop.
 *
 * Algorithms:
 *   MK = PBKDF2-SHA256(password, salt, 600_000) → 32 bytes
 *   subkey = HKDF-SHA256(MK, salt=zeros[32], info, 32 bytes)
 *   AES-GCM with 12-byte random nonce, 128-bit tag, no AAD
 *
 * Wrapped blob layout: nonce(12) || ciphertext || tag(16)
 */
object Crypto {

    private const val KDF_ITERATIONS = 600_000
    private const val GCM_TAG_BITS = 128
    private const val NONCE_LEN = 12
    private const val SALT_LEN = 16
    private const val DEK_LEN = 32

    private val random = SecureRandom()

    // ===== Hex =====

    fun toHex(bytes: ByteArray): String =
        buildString(bytes.size * 2) {
            for (b in bytes) {
                val v = b.toInt() and 0xff
                append(HEX[v ushr 4])
                append(HEX[v and 0x0f])
            }
        }

    fun fromHex(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "invalid hex length" }
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            out[i] = ((digit(hex[i * 2]) shl 4) or digit(hex[i * 2 + 1])).toByte()
        }
        return out
    }

    private fun digit(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> throw IllegalArgumentException("bad hex char: $c")
    }

    private val HEX = "0123456789abcdef".toCharArray()

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { random.nextBytes(it) }

    fun generateSalt(): ByteArray = randomBytes(SALT_LEN)
    fun generateDek(): ByteArray = randomBytes(DEK_LEN)
    fun generateRecoveryKey(): ByteArray = randomBytes(32)

    // ===== KDF =====

    fun pbkdf2(password: String, salt: ByteArray, iterations: Int = KDF_ITERATIONS, length: Int = 32): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, length * 8)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    /** HKDF-SHA256 with empty 32-byte salt, matching client. */
    fun hkdf(ikm: ByteArray, info: String, length: Int = 32): ByteArray {
        val salt = ByteArray(32) // zero-filled, same as client
        val mac = Mac.getInstance("HmacSHA256")
        // Extract
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = mac.doFinal(ikm)
        // Expand
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        val infoBytes = info.toByteArray(Charsets.UTF_8)
        val out = ByteArray(length)
        var t = ByteArray(0)
        var pos = 0
        var i = 1
        while (pos < length) {
            mac.reset()
            mac.update(t)
            mac.update(infoBytes)
            mac.update(byteArrayOf(i.toByte()))
            t = mac.doFinal()
            val take = minOf(t.size, length - pos)
            System.arraycopy(t, 0, out, pos, take)
            pos += take
            i++
        }
        return out
    }

    data class DerivedKeys(val authKeyHex: String, val kek: ByteArray)

    fun deriveKeysFromPassword(password: String, salt: ByteArray): DerivedKeys {
        val mk = pbkdf2(password, salt)
        val authKey = hkdf(mk, "trackhub-auth")
        val kek = hkdf(mk, "trackhub-enc")
        return DerivedKeys(toHex(authKey), kek)
    }

    data class RecoveryKeys(val kek: ByteArray, val authKeyHex: String)

    fun deriveRecoveryKeys(recoveryKey: ByteArray): RecoveryKeys {
        val kek = hkdf(recoveryKey, "trackhub-recovery")
        val auth = hkdf(recoveryKey, "trackhub-recovery-auth")
        return RecoveryKeys(kek, toHex(auth))
    }

    // ===== AES-GCM =====

    /** Encrypt `plaintext` with `key` and given `nonce` (12 bytes). Returns ciphertext||tag. */
    fun aesGcmEncrypt(plaintext: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        require(nonce.size == NONCE_LEN) { "nonce must be 12 bytes" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return cipher.doFinal(plaintext)
    }

    /** Decrypt `ciphertext||tag` with `key` and `nonce`. Returns plaintext. */
    fun aesGcmDecrypt(ciphertextWithTag: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
        require(nonce.size == NONCE_LEN) { "nonce must be 12 bytes" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return cipher.doFinal(ciphertextWithTag)
    }

    /** Wrap a value: returns `nonce(12) || ciphertext||tag`. */
    fun wrapBytes(plaintext: ByteArray, key: ByteArray): ByteArray {
        val nonce = randomBytes(NONCE_LEN)
        val ct = aesGcmEncrypt(plaintext, key, nonce)
        return nonce + ct
    }

    /** Unwrap `nonce(12) || ciphertext||tag` back to plaintext. */
    fun unwrapBytes(blob: ByteArray, key: ByteArray): ByteArray {
        require(blob.size > NONCE_LEN + 16) { "wrapped blob too short" }
        val nonce = blob.sliceArray(0 until NONCE_LEN)
        val ct = blob.sliceArray(NONCE_LEN until blob.size)
        return aesGcmDecrypt(ct, key, nonce)
    }

    // ===== Record-shaped encryption =====

    data class EncryptedBlob(val nonceHex: String, val ciphertextHex: String)

    fun encryptRecord(jsonBytes: ByteArray, dek: ByteArray): EncryptedBlob {
        val nonce = randomBytes(NONCE_LEN)
        val ct = aesGcmEncrypt(jsonBytes, dek, nonce)
        return EncryptedBlob(toHex(nonce), toHex(ct))
    }

    fun decryptRecord(blob: EncryptedBlob, dek: ByteArray): ByteArray =
        aesGcmDecrypt(fromHex(blob.ciphertextHex), dek, fromHex(blob.nonceHex))
}
