package com.trackhub.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Provides encrypted SharedPreferences instances backed by a hardware-backed
 * master key from the Android Keystore.
 *
 * Two prefs files:
 *   - "trackhub_cookies": HTTP cookie store (session, csrf_token).
 *   - "trackhub_dek": wrapped DEK + meta (managed by DekManager).
 */
object SessionStore {

    private const val COOKIES_FILE = "trackhub_cookies"
    private const val DEK_FILE = "trackhub_dek"

    @Volatile private var cookies: SharedPreferences? = null
    @Volatile private var dek: SharedPreferences? = null

    private fun masterKey(context: Context): MasterKey =
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    fun cookies(context: Context): SharedPreferences =
        cookies ?: synchronized(this) {
            cookies ?: open(context, COOKIES_FILE).also { cookies = it }
        }

    fun dek(context: Context): SharedPreferences =
        dek ?: synchronized(this) {
            dek ?: open(context, DEK_FILE).also { dek = it }
        }

    private fun open(context: Context, name: String): SharedPreferences =
        EncryptedSharedPreferences.create(
            context.applicationContext,
            name,
            masterKey(context),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
}
