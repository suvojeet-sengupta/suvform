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
import androidx.compose.ui.graphics.Color
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
import com.suvojeetsengupta.suvform.ui.theme.Fraunces
import com.suvojeetsengupta.suvform.ui.theme.Mono
import com.suvojeetsengupta.suvform.ui.theme.SuvTheme

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
    val c = SuvTheme.colors

    LaunchedEffect(state.success) {
        if (state.success) {
            onSignedIn()
            viewModel.consumeSuccess()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(34.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(c.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("S", color = c.onAccent, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium, fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text("SuvForm", fontFamily = Fraunces, fontSize = 22.sp, letterSpacing = (-0.4).sp, color = c.ink)
            }

            Spacer(Modifier.height(56.dp))

            Text(
                buildAnnotatedString {
                    append("Forms, ")
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("drafted") }
                    append(" by AI")
                    withStyle(SpanStyle(color = c.accent)) { append(".") }
                    append("\nOwned by ")
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("you") }
                    withStyle(SpanStyle(color = c.accent)) { append(".") }
                },
                fontFamily = Fraunces,
                fontWeight = FontWeight.Light,
                fontSize = 40.sp,
                lineHeight = 41.sp,
                letterSpacing = (-1).sp,
                color = c.ink,
            )

            Spacer(Modifier.height(36.dp))

            Column {
                features.forEach { f ->
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                    Row(Modifier.padding(vertical = 14.dp)) {
                        Text(f.num, fontFamily = Mono, fontSize = 11.sp, color = c.muted, modifier = Modifier.width(28.dp))
                        Column {
                            Text(f.title, fontFamily = MaterialTheme.typography.bodyMedium.fontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = c.ink, lineHeight = 18.sp)
                            Text(f.sub, style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
            }

            Spacer(Modifier.weight(1f))

            Surface(
                onClick = { viewModel.signIn(context) },
                enabled = !state.loading && !state.success,
                shape = RoundedCornerShape(16.dp),
                color = c.feature,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (state.loading || state.success) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = c.onFeature, strokeWidth = 2.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GoogleGlyph()
                            Spacer(Modifier.width(12.dp))
                            Text("Continue with Google", color = c.onFeature, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.error != null, enter = fadeIn() + expandVertically()) {
                state.error?.let {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        Text(it, color = c.accent, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "By continuing, you agree to our Terms of Service and Privacy Policy.",
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = c.muted2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun GoogleGlyph() {
    Surface(modifier = Modifier.size(22.dp), shape = CircleShape, color = Color.White) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF4285F4), fontWeight = FontWeight.Black)) { append("G") }
                },
                fontSize = 13.sp,
            )
        }
    }
}
