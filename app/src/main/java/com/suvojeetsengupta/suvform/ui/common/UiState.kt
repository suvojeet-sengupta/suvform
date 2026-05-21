package com.suvojeetsengupta.suvform.ui.common

/**
 * One state model for any data-bearing screen, so loading / error / empty /
 * content are handled consistently everywhere (and a flow that never emits
 * can't silently get stuck on Loading — the bug we hit on the Responses screen).
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Content<out T>(val data: T) : UiState<T>
}

/** Convenience: wrap a (possibly empty) list into Empty/Content. */
fun <T> List<T>.toUiState(): UiState<List<T>> =
    if (isEmpty()) UiState.Empty else UiState.Content(this)
