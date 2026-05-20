package com.suvojeetsengupta.suvform.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
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

    LaunchedEffect(state.success) { if (state.success) onSignedIn() }

    // Trigger entry animation once.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brand_BackgroundBrush()),
    ) {
        // Decorative blurred blobs (subtle, drawn as gradients)
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(brand_AccentPurple.copy(alpha = 0.35f), Color.Transparent),
                    ),
                    shape = CircleShape,
                )
                .align(Alignment.TopStart),
        )
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(brand_AccentCyan.copy(alpha = 0.30f), Color.Transparent),
                    ),
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
                visible = visible,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 6 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LogoBadge()
                    Spacer(Modifier.height(24.dp))
                    Wordmark()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Forms, reimagined with AI",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(32.dp))
                    FeatureChips()
                }
            }

            Spacer(Modifier.height(48.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700, delayMillis = 200)) +
                        slideInVertically(tween(700, delayMillis = 200)) { it / 4 },
            ) {
                GoogleSignInButton(
                    loading = state.loading,
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
    }
}

// -------------------- Logo / Wordmark --------------------

@Composable
private fun LogoBadge() {
    // Animated subtle scale so the sparkle "breathes".
    val transition = rememberInfiniteTransition(label = "logo")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logo_pulse",
    )

    Box(
        modifier = Modifier
            .size(96.dp)
            .shadow(elevation = 18.dp, shape = CircleShape, ambientColor = brand_AccentPurple, spotColor = brand_AccentPurple)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(brand_AccentPurple, brand_AccentCyan),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "✦",
            fontSize = (44 * pulse).sp,
            color = Color.White,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun Wordmark() {
    val gradientText = buildAnnotatedString {
        withStyle(
            SpanStyle(
                brush = Brush.linearGradient(listOf(brand_AccentPurple, brand_AccentCyan)),
                fontWeight = FontWeight.ExtraBold,
            ),
        ) {
            append("SuvForm")
        }
    }
    Text(
        text = gradientText,
        fontSize = 44.sp,
        letterSpacing = (-1).sp,
        textAlign = TextAlign.Center,
    )
}

// -------------------- Feature chips --------------------

@Composable
private fun FeatureChips() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FeatureChip("✨ AI-built")
        FeatureChip("∑ Smart math")
        FeatureChip("🔒 Private")
    }
}

@Composable
private fun FeatureChip(label: String) {
    Surface(
        shape = RoundedCornerShape(100),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )
    }
}

// -------------------- Google sign-in button --------------------

@Composable
private fun GoogleSignInButton(
    loading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
            )
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(loading) {
                if (!loading) detectTapGestures(onTap = { onClick() })
            },
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = brand_AccentPurple,
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    "Signing in…",
                    color = Color(0xFF1F1F1F),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            } else {
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
}

/**
 * A simple Google-style "G" rendered as text — 4-color word built with annotated spans.
 * Trademark-clean: not the actual Google asset, but recognisable enough.
 */
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

// -------------------- Brand colors --------------------

private val brand_AccentPurple = Color(0xFF7C5CFF)
private val brand_AccentCyan = Color(0xFF22D3EE)

@Composable
private fun brand_BackgroundBrush(): Brush {
    val base = MaterialTheme.colorScheme.background
    return Brush.verticalGradient(
        0f to base,
        0.55f to base,
        1f to MaterialTheme.colorScheme.surface,
    )
}
