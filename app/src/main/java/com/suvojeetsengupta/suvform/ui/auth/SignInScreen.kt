package com.suvojeetsengupta.suvform.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeetsengupta.suvform.ui.theme.Accent
import com.suvojeetsengupta.suvform.ui.theme.CardWhite
import com.suvojeetsengupta.suvform.ui.theme.Fraunces
import com.suvojeetsengupta.suvform.ui.theme.Ink
import com.suvojeetsengupta.suvform.ui.theme.Line
import com.suvojeetsengupta.suvform.ui.theme.Mono
import com.suvojeetsengupta.suvform.ui.theme.Muted
import com.suvojeetsengupta.suvform.ui.theme.Muted2

private data class Feature(val num: String, val title: String, val sub: String)

private val features = listOf(
    Feature("01", "Generate from a sentence", "Describe the form in plain language — AI drafts the fields."),
    Feature("02", "Calculations built in", "Totals, taxes and scores compute themselves on every response."),
    Feature("03", "Private by default", "Forms are yours. Share a link only when you choose to publish."),
)

@Composable
fun SignInScreen(
    onSignedIn: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.success) {
        if (state.success) {
            onSignedIn()
            viewModel.consumeSuccess()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(34.dp))

            // Logo block
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("S", color = CardWhite, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium, fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text("SuvForm", fontFamily = Fraunces, fontSize = 22.sp, letterSpacing = (-0.4).sp, color = Ink)
            }

            Spacer(Modifier.height(56.dp))

            // Super headline
            Text(
                buildAnnotatedString {
                    append("Forms, ")
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("drafted") }
                    append(" by AI")
                    withStyle(SpanStyle(color = Accent)) { append(".") }
                    append("\nOwned by ")
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("you") }
                    withStyle(SpanStyle(color = Accent)) { append(".") }
                },
                fontFamily = Fraunces,
                fontWeight = FontWeight.Light,
                fontSize = 40.sp,
                lineHeight = 41.sp,
                letterSpacing = (-1).sp,
                color = Ink,
            )

            Spacer(Modifier.height(36.dp))

            // Numbered features
            Column {
                features.forEach { f ->
                    HorizontalHair()
                    Row(Modifier.padding(vertical = 14.dp)) {
                        Text(f.num, fontFamily = Mono, fontSize = 11.sp, color = Muted, modifier = Modifier.width(28.dp))
                        Column {
                            Text(f.title, fontFamily = MaterialTheme.typography.bodyMedium.fontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Ink, lineHeight = 18.sp)
                            Text(f.sub, style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
                HorizontalHair()
            }

            Spacer(Modifier.weight(1f))

            // CTA
            Surface(
                onClick = { viewModel.signIn(context) },
                enabled = !state.loading && !state.success,
                shape = RoundedCornerShape(16.dp),
                color = Ink,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (state.loading || state.success) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = CardWhite, strokeWidth = 2.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GoogleGlyph()
                            Spacer(Modifier.width(12.dp))
                            Text("Continue with Google", color = CardWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.error != null, enter = fadeIn() + expandVertically()) {
                state.error?.let {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        Text(it, color = Accent, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "By continuing, you agree to our Terms of Service and Privacy Policy.",
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = Muted2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@androidx.compose.runtime.Composable
private fun HorizontalHair() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}

@androidx.compose.runtime.Composable
private fun GoogleGlyph() {
    Surface(modifier = Modifier.size(22.dp), shape = CircleShape, color = CardWhite) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF4285F4), fontWeight = FontWeight.Black)) { append("G") }
                },
                fontSize = 13.sp,
            )
        }
    }
}
