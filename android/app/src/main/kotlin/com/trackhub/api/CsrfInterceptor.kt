package com.trackhub.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Echoes the `csrf_token` cookie back as the `X-CSRF-Token` header on
 * state-changing requests, matching the server's double-submit CSRF check.
 */
class CsrfInterceptor(private val cookieStore: CookieJarStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val isStateChanging = req.method !in SAFE_METHODS
        if (!isStateChanging) return chain.proceed(req)

        val csrf = cookieStore.cookie(req.url.host, "csrf_token") ?: return chain.proceed(req)
        val newReq = req.newBuilder().header("X-CSRF-Token", csrf).build()
        return chain.proceed(newReq)
    }

    companion object {
        private val SAFE_METHODS = setOf("GET", "HEAD", "OPTIONS")
    }
}
