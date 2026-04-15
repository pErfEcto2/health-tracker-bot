package com.trackhub.api

import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Persistent cookie jar backed by EncryptedSharedPreferences. Tracks one cookie set
 * per host, which is enough for our single-domain client.
 */
class CookieJarStore(private val prefs: SharedPreferences) : CookieJar {

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val existing = loadCookies(url.host).associateBy { it.name }.toMutableMap()
        for (c in cookies) {
            // Drop expired cookies the server clears.
            if (c.expiresAt < System.currentTimeMillis() && !c.persistent) {
                existing.remove(c.name)
                continue
            }
            existing[c.name] = c
        }
        store(url.host, existing.values.toList())
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return loadCookies(url.host)
            .filter { it.expiresAt > now }
            .filter { it.matches(url) }
    }

    /** Read a cookie value (e.g. csrf_token) without OkHttp request context. */
    @Synchronized
    fun cookie(host: String, name: String): String? =
        loadCookies(host).firstOrNull { it.name == name }?.value

    @Synchronized
    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun loadCookies(host: String): List<Cookie> {
        val raw = prefs.getStringSet(host, null) ?: return emptyList()
        return raw.mapNotNull { encoded ->
            // Encoded as "Set-Cookie" string; OkHttp can parse it back.
            val httpUrl = HttpUrl.Builder().scheme("https").host(host).build()
            Cookie.parse(httpUrl, encoded)
        }
    }

    private fun store(host: String, cookies: List<Cookie>) {
        val set = cookies.map { it.toString() }.toSet()
        prefs.edit().putStringSet(host, set).apply()
    }
}
