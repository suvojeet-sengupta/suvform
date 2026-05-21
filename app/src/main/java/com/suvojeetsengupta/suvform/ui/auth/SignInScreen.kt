package com.suvojeetsengupta.suvform.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(state.success) {
        if (state.success) {
            onSignedIn()
            viewModel.consumeSuccess()
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val brandPurple = Color(0xFF7C5CFF)
    val brandCyan = Color(0xFF22D3EE)
    val aiGradient = Brush.linearGradient(listOf(brandPurple, brandCyan))
    
    val textColorPrimary = MaterialTheme.colorScheme.onBackground
    val textColorSecondary = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // AI Decorative Blobs - Reduced alpha for dark mode to keep it subtle
        val blobAlpha = if (isDark) 0.08f else 0.12f
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-50).dp)
                .background(
                    Brush.radialGradient(listOf(brandPurple.copy(alpha = blobAlpha), Color.Transparent)),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 120.dp, y = 80.dp)
                .background(
                    Brush.radialGradient(listOf(brandCyan.copy(alpha = blobAlpha + 0.03f), Color.Transparent)),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // Ensures content doesn't overlap with transparent status bar
                .navigationBarsPadding() // Ensures content doesn't overlap with navigation bar
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo + Branding (Professional Layout with AI Gradient)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .shadow(
                            elevation = 12.dp, 
                            shape = RoundedCornerShape(24.dp), 
                            ambientColor = brandPurple, 
                            spotColor = brandPurple
                        )
                        .background(aiGradient, shape = RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                val wordmark = buildAnnotatedString {
                    withStyle(SpanStyle(brush = aiGradient, fontWeight = FontWeight.Bold)) {
                        append("SuvForm")
                    }
                }
                Text(
                    text = wordmark,
                    fontSize = 36.sp,
                    letterSpacing = (-1).sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Create. Share. Understand.",
                    fontSize = 17.sp,
                    color = textColorSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Main Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Text(
                    text = "Sign in to start building\nintelligent forms",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColorPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Professional Sign In Button with AI Gradient
                Surface(
                    onClick = { viewModel.signIn(context) },
                    enabled = !state.loading && !state.success,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(
                            elevation = 8.dp, 
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = if (isDark) Color.Black else brandPurple.copy(alpha = 0.5f)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent, // Using transparent to show gradient Box below
                    border = null
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(aiGradient)
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.loading || state.success) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                GoogleGlyph()
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Continue with Google",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Error Message
                AnimatedVisibility(
                    visible = state.error != null,
                    enter = fadeIn() + expandVertically(),
                ) {
                    state.error?.let {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "By continuing, you agree to our Terms of Service\nand Privacy Policy.",
                    fontSize = 12.sp,
                    color = textColorSecondary.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun GoogleGlyph() {
    Surface(
        modifier = Modifier.size(24.dp),
        shape = CircleShape,
        color = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) {
            val g = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFF4285F4), fontWeight = FontWeight.Black)) { append("G") }
            }
            Text(text = g, fontSize = 14.sp)
        }
    }
}
