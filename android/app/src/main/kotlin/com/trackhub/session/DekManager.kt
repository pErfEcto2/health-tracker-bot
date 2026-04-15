package com.trackhub.session

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.trackhub.crypto.Crypto
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Hardware-backed wrapping of the data encryption key (DEK).
 *
 * - Wrap key alias `trackhub_dek_wrap` lives in Android Keystore, requires
 *   BIOMETRIC_STRONG for every operation (no auth-validity timeout).
 * - Wrapped DEK + nonce are stored in EncryptedSharedPreferences.
 * - Each persist or unlock requires a `BiometricPrompt` with the cipher as
 *   CryptoObject — the auth is bound to the specific cipher instance.
 *
 * In-memory cache (volatile) holds the unwrapped DEK between unwrap and clear.
 */
class DekManager(private val context: Context) {

    private val prefs: SharedPreferences get() = SessionStore.dek(context)

    @Volatile private var cached: ByteArray? = null

    fun hasWrappedDek(): Boolean = prefs.contains(KEY_WRAPPED) && prefs.contains(KEY_NONCE)
    fun hasMemoryDek(): Boolean = cached != null
    fun memoryDek(): ByteArray? = cached

    fun setDekFromUnlock(dek: ByteArray) { cached = dek }

    fun clear() {
        cached = null
        prefs.edit().clear().apply()
        runCatching {
            val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            ks.deleteEntry(WRAP_ALIAS)
        }
    }

    fun canUseBiometric(): Boolean {
        val mgr = BiometricManager.from(context)
        return mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    /** Persist the in-memory DEK with a biometric-bound encrypt cipher. */
    suspend fun persistWithBiometric(activity: FragmentActivity, dek: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrapKey())
        val authed = promptBiometric(
            activity = activity,
            title = "Запомнить ключ",
            subtitle = "Подтверди отпечатком, чтобы сохранить",
            crypto = BiometricPrompt.CryptoObject(cipher),
        )
        val ct = authed.doFinal(dek)
        prefs.edit()
            .putString(KEY_WRAPPED, Crypto.toHex(ct))
            .putString(KEY_NONCE, Crypto.toHex(authed.iv))
            .apply()
        cached = dek
    }

    /** Show biometric prompt; on success unlock and cache the DEK in memory. */
    suspend fun unlockWithBiometric(activity: FragmentActivity): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val nonce = Crypto.fromHex(prefs.getString(KEY_NONCE, null) ?: error("no wrapped DEK"))
        cipher.init(Cipher.DECRYPT_MODE, getWrapKey(), GCMParameterSpec(128, nonce))

        val authed = promptBiometric(
            activity = activity,
            title = "Войти в TrackHub",
            subtitle = "Подтверди отпечатком",
            crypto = BiometricPrompt.CryptoObject(cipher),
        )
        val ct = Crypto.fromHex(prefs.getString(KEY_WRAPPED, null) ?: error("no wrapped DEK"))
        val dek = authed.doFinal(ct)
        cached = dek
        return dek
    }

    private suspend fun promptBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        crypto: BiometricPrompt.CryptoObject,
    ): Cipher = suspendCancellableCoroutine { cont ->
        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val cipher = result.cryptoObject?.cipher
                if (cipher != null) cont.resume(cipher)
                else cont.resumeWithException(IllegalStateException("biometric: no cipher"))
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                cont.resumeWithException(BiometricException(errorCode, errString.toString()))
            }
            override fun onAuthenticationFailed() { /* soft fail; let user retry */ }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Отмена")
            .build()
        prompt.authenticate(info, crypto)
    }

    private fun getOrCreateWrapKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = ks.getEntry(WRAP_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(
                WRAP_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return gen.generateKey()
    }

    private fun getWrapKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val entry = ks.getEntry(WRAP_ALIAS, null) as? KeyStore.SecretKeyEntry
            ?: error("wrap key missing")
        return entry.secretKey
    }

    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val WRAP_ALIAS = "trackhub_dek_wrap"
        private const val KEY_WRAPPED = "wrapped_dek"
        private const val KEY_NONCE = "wrap_nonce"
    }
}

class BiometricException(val errorCode: Int, message: String) : RuntimeException(message)
