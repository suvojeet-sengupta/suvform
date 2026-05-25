package com.suvojeetsengupta.suvform.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeetsengupta.suvform.R
import com.suvojeetsengupta.suvform.ui.components.ButtonVariant
import com.suvojeetsengupta.suvform.ui.components.ChipVariant
import com.suvojeetsengupta.suvform.ui.components.SectionLabel
import com.suvojeetsengupta.suvform.ui.components.SuvButton
import com.suvojeetsengupta.suvform.ui.components.SuvCard
import com.suvojeetsengupta.suvform.ui.components.SuvChip
import com.suvojeetsengupta.suvform.ui.theme.Fraunces
import com.suvojeetsengupta.suvform.ui.theme.Mono
import com.suvojeetsengupta.suvform.ui.theme.SuvTheme

private val examples = listOf(
    "Quotation form for interior design with item, quantity, unit price and an auto-calculated total.",
    "Customer feedback survey with a 1–5 satisfaction rating and an open comment field.",
    "Event registration with name, email, ticket type and number of guests.",
)

@Composable
fun CreateScreen(
    onBack: () -> Unit,
    onOpenEditor: () -> Unit,
    viewModel: CreateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = SuvTheme.colors

    LaunchedEffect(state.navigateToEditor) {
        if (state.navigateToEditor) {
            onOpenEditor()
            viewModel.onNavigated()
        }
    }

    Scaffold(containerColor = c.paper) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 96.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = c.ink) }
                    Spacer(Modifier.width(4.dp))
                    Text("Back", style = MaterialTheme.typography.bodyMedium, color = c.ink)
                }

                // AI hero (feature surface)
                SuvCard(radius = 24, border = false, container = c.feature, contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 40.dp)) {
                    Column {
                        SectionLabel("AI Builder", color = c.onFeature.copy(alpha = 0.6f), tick = true)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            buildAnnotatedString {
                                append("Describe your ")
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("form") }
                                withStyle(SpanStyle(color = c.accent)) { append(",") }
                                append("\nwe'll draft it.")
                            },
                            fontFamily = Fraunces,
                            fontSize = 30.sp,
                            lineHeight = 32.sp,
                            letterSpacing = (-0.6).sp,
                            color = c.onFeature,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("AI turns a sentence into fields and calculations you can refine.", style = MaterialTheme.typography.bodyMedium, color = c.onFeature.copy(alpha = 0.7f))
                    }
                }

                // Prompt card overlapping the hero
                Box(Modifier.offset(y = (-28).dp)) {
                    SuvCard(radius = 20, contentPadding = PaddingValues(16.dp)) {
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                SectionLabel("Your prompt")
                                Text("${state.prompt.length}/3000", fontFamily = Mono, fontSize = 10.sp, color = c.muted2)
                            }
                            Spacer(Modifier.height(10.dp))
                            BasicTextField(
                                value = state.prompt,
                                onValueChange = viewModel::updatePrompt,
                                enabled = !state.loading,
                                textStyle = TextStyle(fontFamily = MaterialTheme.typography.bodyLarge.fontFamily, fontSize = 14.sp, lineHeight = 21.sp, color = c.ink),
                                cursorBrush = SolidColor(c.accent),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp),
                                decorationBox = { inner ->
                                    if (state.prompt.isEmpty()) {
                                        Text("e.g. Quotation form for interior design with item, qty, price and total", fontSize = 14.sp, lineHeight = 21.sp, color = c.muted2)
                                    }
                                    inner()
                                },
                            )
                            Spacer(Modifier.height(14.dp))
                            Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Language", style = MaterialTheme.typography.bodySmall, color = c.muted)
                                Spacer(Modifier.width(10.dp))
                                Box(Modifier.clip(RoundedCornerShape(100.dp)).clickable { viewModel.setLocale("en") }) {
                                    SuvChip("English", if (state.locale == "en") ChipVariant.SolidInk else ChipVariant.Outline)
                                }
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.clip(RoundedCornerShape(100.dp)).clickable { viewModel.setLocale("hi") }) {
                                    SuvChip("हिन्दी", if (state.locale == "hi") ChipVariant.SolidInk else ChipVariant.Outline)
                                }
                            }
                        }
                    }
                }

                state.error?.let { msg ->
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.accentSoft).padding(14.dp)) {
                        Text(msg, style = MaterialTheme.typography.bodyMedium, color = c.accentDeep)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                SectionLabel("Try an example")
                Spacer(Modifier.height(12.dp))
                examples.forEachIndexed { i, ex ->
                    Box(Modifier.clip(RoundedCornerShape(14.dp)).clickable(enabled = !state.loading) { viewModel.updatePrompt(ex) }) {
                        SuvCard(radius = 14, contentPadding = PaddingValues(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Row {
                                Text("0${i + 1}", fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = c.accent, modifier = Modifier.width(22.dp).padding(top = 2.dp))
                                Text(ex, style = MaterialTheme.typography.bodyMedium, color = c.ink, lineHeight = 18.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { viewModel.startBlank() }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text(
                        buildAnnotatedString {
                            append("or ")
                            withStyle(SpanStyle(color = c.ink, fontWeight = FontWeight.SemiBold)) { append("start from scratch") }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.muted,
                    )
                }
            }

            SuvButton(
                text = if (state.loading) "Generating…" else "Generate draft",
                onClick = viewModel::generate,
                enabled = !state.loading && state.prompt.isNotBlank(),
                variant = ButtonVariant.Accent,
                leading = if (state.loading) null else R.drawable.ic_auto_awesome,
                height = 54,
                radius = 16,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}
