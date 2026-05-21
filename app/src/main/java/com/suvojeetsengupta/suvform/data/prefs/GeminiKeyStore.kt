package com.suvojeetsengupta.suvform.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the user's optional "bring your own" Gemini API key.
 * Shared between Settings (read/write) and the network interceptor (read), so a key
 * entered in Settings is sent on AI requests via the X-Gemini-Key header.
 */
@Singleton
class GeminiKeyStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("suvform_prefs", Context.MODE_PRIVATE)

    private val _key = MutableStateFlow(prefs.getString(KEY, "").orEmpty())
    val key: StateFlow<String> = _key.asStateFlow()

    fun setKey(value: String) {
        val trimmed = value.trim()
        prefs.edit().putString(KEY, trimmed).apply()
        _key.value = trimmed
    }

    companion object {
        private const val KEY = "gemini_api_key"
    }
}
