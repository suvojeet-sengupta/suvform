package com.suvojeetsengupta.suvform.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException

/**
 * Turns raw exceptions into user-friendly messages. Maps the worker's machine
 * error codes (e.g. "quota_exceeded") to plain language and falls back to a
 * sensible message per HTTP status / network error — so screens never surface
 * "HTTP 502" to the user.
 */
object ErrorMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun message(e: Throwable): String = when (e) {
        is HttpException -> httpMessage(e)
        is IOException -> "No internet connection. Check your network and try again."
        else -> e.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
    }

    private fun httpMessage(e: HttpException): String {
        val code = runCatching { e.response()?.errorBody()?.string() }
            .getOrNull()
            ?.let { body ->
                runCatching {
                    json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content
                }.getOrNull()
            }
        return when (code) {
            "quota_exceeded" -> "You've reached today's AI generation limit. Try again tomorrow."
            "rate_limited" -> "Too many requests. Please wait a moment and try again."
            "token_revoked" -> "Your session has ended. Please sign in again."
            "invalid_token", "missing_token" -> "Your session expired. Please sign in again."
            "no_gemini_key" -> "No Gemini API key is configured. Add one in Settings."
            "prompt_too_short" -> "Your prompt is too short — add a little more detail."
            "prompt_too_long" -> "Your prompt is too long. Please shorten it."
            "generation_failed" -> "AI couldn't generate the form. Please try again."
            "insights_failed" -> "Couldn't generate insights right now. Please try again later."
            "forbidden" -> "You don't have access to this form."
            "not_found" -> "This form no longer exists."
            "payload_too_large" -> "That submission is too large."
            "title_too_long", "description_too_long", "too_many_fields", "too_many_calculations" ->
                "This form is too large to save. Please reduce its size."
            else -> when (e.code()) {
                401 -> "Your session expired. Please sign in again."
                403 -> "You don't have permission to do that."
                404 -> "Not found."
                429 -> "Too many requests. Please slow down."
                in 500..599 -> "Server error. Please try again shortly."
                else -> "Request failed. Please try again."
            }
        }
    }
}
