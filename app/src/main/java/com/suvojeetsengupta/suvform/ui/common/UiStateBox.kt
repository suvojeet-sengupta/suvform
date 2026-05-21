package com.suvojeetsengupta.suvform.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suvojeetsengupta.suvform.ui.theme.SuvTheme

/**
 * Renders a [UiState] with a consistent look across screens:
 * a centered spinner, an error with an optional Retry, an empty message, or
 * the caller's content. Use this instead of hand-rolling loading/error blocks.
 */
@Composable
fun <T> UiStateBox(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    emptyMessage: String = "Nothing here yet.",
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    val colors = SuvTheme.colors
    when (state) {
        is UiState.Loading -> Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp), color = colors.accent)
        }

        is UiState.Error -> Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.message,
                color = colors.muted,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry) { Text("Retry", color = colors.accent) }
            }
        }

        is UiState.Empty -> Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emptyMessage, color = colors.muted, textAlign = TextAlign.Center)
        }

        is UiState.Content -> content(state.data)
    }
}
