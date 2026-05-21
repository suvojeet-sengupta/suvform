package com.suvojeetsengupta.suvform.data.remote

import com.suvojeetsengupta.suvform.data.prefs.GeminiKeyStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the user's personal Gemini key (`X-Gemini-Key`) to AI requests only.
 * The worker uses this key if present and otherwise falls back to its own secret,
 * so users can optionally bring their own key from Settings.
 */
@Singleton
class GeminiKeyInterceptor @Inject constructor(
    private val keyStore: GeminiKeyStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val isAiRequest = path.contains("/v1/ai/") || path.endsWith("/insights")

        val key = keyStore.key.value
        return if (isAiRequest && key.isNotBlank()) {
            chain.proceed(request.newBuilder().header("X-Gemini-Key", key).build())
        } else {
            chain.proceed(request)
        }
    }
}
