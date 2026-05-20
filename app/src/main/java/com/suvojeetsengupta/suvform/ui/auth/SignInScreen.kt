package com.suvojeetsengupta.suvform.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SignInScreen(
    onSignedIn: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // On success: fire onSignedIn ONCE and clear state to avoid re-firing on recomposition.
    LaunchedEffect(state.success) {
        if (state.success) {
            onSignedIn()
            viewModel.consumeSuccess()
        }
    }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brandBackgroundBrush()),
    ) {
        // Decorative blurred blobs
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    Brush.radialGradient(listOf(BrandPurple.copy(alpha = 0.30f), Color.Transparent)),
                    shape = CircleShape,
                )
                .align(Alignment.TopStart),
        )
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    Brush.radialGradient(listOf(BrandCyan.copy(alpha = 0.28f), Color.Transparent)),
                    shape = CircleShape,
                )
                .align(Alignment.BottomEnd),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 6 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LogoBadge()
                    Spacer(Modifier.height(24.dp))
                    Wordmark()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Sign in to manage your forms and responses",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(700, delayMillis = 200)) +
                        slideInVertically(tween(700, delayMillis = 200)) { it / 4 },
            ) {
                GoogleSignInButton(
                    enabled = !state.loading && !state.success,
                    onClick = { viewModel.signIn(context) },
                )
            }

            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn() + expandVertically(),
            ) {
                state.error?.let {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = "By continuing you agree to Terms & Privacy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }

        // Full-screen overlay while signing in — prevents UI flash + double-tap navigation.
        if (state.loading || state.success) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "Signing in…",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

// ----- Logo / Wordmark -----

@Composable
private fun LogoBadge() {
    Box(
        modifier = Modifier
            .size(88.dp)
            .shadow(elevation = 18.dp, shape = CircleShape, ambientColor = BrandPurple, spotColor = BrandPurple)
            .background(
                Brush.linearGradient(listOf(BrandPurple, BrandCyan)),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "S",
            fontSize = 44.sp,
            color = Color.White,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun Wordmark() {
    val gradient = buildAnnotatedString {
        withStyle(
            SpanStyle(
                brush = Brush.linearGradient(listOf(BrandPurple, BrandCyan)),
                fontWeight = FontWeight.ExtraBold,
            ),
        ) { append("SuvForm") }
    }
    Text(
        text = gradient,
        fontSize = 44.sp,
        letterSpacing = (-1).sp,
        textAlign = TextAlign.Center,
    )
}

// ----- Google sign-in button -----

@Composable
private fun GoogleSignInButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.35f),
            )
            .clip(RoundedCornerShape(16.dp)),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            GoogleGlyph()
            Spacer(Modifier.width(14.dp))
            Text(
                "Continue with Google",
                color = Color(0xFF1F1F1F),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun GoogleGlyph() {
    val g = buildAnnotatedString {
        withStyle(SpanStyle(color = Color(0xFF4285F4), fontWeight = FontWeight.Black)) { append("G") }
        withStyle(SpanStyle(color = Color(0xFFEA4335), fontWeight = FontWeight.Black)) { append("o") }
        withStyle(SpanStyle(color = Color(0xFFFBBC05), fontWeight = FontWeight.Black)) { append("o") }
        withStyle(SpanStyle(color = Color(0xFF4285F4), fontWeight = FontWeight.Black)) { append("g") }
        withStyle(SpanStyle(color = Color(0xFF34A853), fontWeight = FontWeight.Black)) { append("l") }
        withStyle(SpanStyle(color = Color(0xFFEA4335), fontWeight = FontWeight.Black)) { append("e") }
    }
    Text(text = g, fontSize = 18.sp)
}

// ----- Brand colors -----

private val BrandPurple = Color(0xFF7C5CFF)
private val BrandCyan = Color(0xFF22D3EE)

@Composable
private fun brandBackgroundBrush(): Brush {
    val base = MaterialTheme.colorScheme.background
    return Brush.verticalGradient(
        0f to base,
        0.55f to base,
        1f to MaterialTheme.colorScheme.surface,
    )
}
