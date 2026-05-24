package com.suvojeetsengupta.suvform.ui.responses

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.suvojeetsengupta.suvform.data.remote.FieldDto
import com.suvojeetsengupta.suvform.data.remote.FormSummaryDto
import com.suvojeetsengupta.suvform.data.remote.ResponseItemDto
import com.suvojeetsengupta.suvform.ui.components.GlyphIcon
import com.suvojeetsengupta.suvform.ui.components.MonoMeta
import com.suvojeetsengupta.suvform.ui.components.SectionLabel
import com.suvojeetsengupta.suvform.ui.components.SuvCard
import com.suvojeetsengupta.suvform.ui.theme.Fraunces
import com.suvojeetsengupta.suvform.ui.theme.Mono
import com.suvojeetsengupta.suvform.ui.theme.SuvTheme
import com.suvojeetsengupta.suvform.util.BiometricAuthManager
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsesScreen(
    onBack: () -> Unit,
    onViewDetail: () -> Unit,
    viewModel: ResponsesViewModel = hiltViewModel<ResponsesViewModel>(),
    biometricAuthManager: BiometricAuthManager = hiltViewModel<BiometricAuthManager>()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val c = SuvTheme.colors
    val lazyPagingItems = viewModel.responsesPagingData.collectAsLazyPagingItems()

    var biometricAuthenticated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isBiometricEnabled) {
            biometricAuthenticated = true
            viewModel.refresh()
        } else {
            biometricAuthManager.authenticate(
                activity = context as FragmentActivity,
                title = "Access Responses",
                subtitle = "Authenticate to view collected data",
                onSuccess = {
                    biometricAuthenticated = true
                    viewModel.refresh()
                },
                onError = { }
            )
        }
    }

    BackHandler(enabled = true) {
        if (state.selectedFormId != null) viewModel.clearSelection() else onBack()
    }

    var exportMenuOpen by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()

    Scaffold(
        containerColor = c.paper,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.formTitle.ifBlank { "Responses" },
                            fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.selectedFormId != null) {
                            MonoMeta("${state.totalCount} ${if (state.totalCount == 1) "response" else "responses"}", color = c.muted)
                        }
                    }
                },
                navigationIcon = {
                    if (state.selectedFormId != null) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.ink)
                        }
                    }
                },
                actions = {
                    if (state.selectedFormId != null) {
                        Box {
                            IconButton(onClick = { exportMenuOpen = true }, enabled = lazyPagingItems.itemCount > 0 && !state.exporting) {
                                if (state.exporting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = c.ink)
                                else Icon(Icons.Filled.IosShare, "Export", tint = c.ink)
                            }
                            DropdownMenu(expanded = exportMenuOpen, onDismissRequest = { exportMenuOpen = false }) {
                                DropdownMenuItem(text = { Text("Export as CSV") }, leadingIcon = { Icon(Icons.Filled.TableChart, null) }, onClick = { exportMenuOpen = false; viewModel.exportCsv(context) })
                                DropdownMenuItem(text = { Text("Export as PDF") }, leadingIcon = { Icon(Icons.Filled.PictureAsPdf, null) }, onClick = { exportMenuOpen = false; viewModel.exportPdf(context) })
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.paper),
            )
        },
    ) { padding ->
        if (isBiometricEnabled && !biometricAuthenticated) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 16.dp),
                        tint = c.ink
                    )
                    Text("Responses are Locked", fontFamily = Fraunces, fontSize = 22.sp, color = c.ink)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            biometricAuthManager.authenticate(
                                activity = context as FragmentActivity,
                                title = "Access Responses",
                                subtitle = "Authenticate to view collected data",
                                onSuccess = {
                                    biometricAuthenticated = true
                                    viewModel.refresh()
                                },
                                onError = { }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = c.ink, contentColor = c.paper)
                    ) {
                        Text("Unlock with Biometrics")
                    }
                }
            }
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount > 0,
            onRefresh = { lazyPagingItems.refresh() },
            state = refreshState,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                // No form selected: form picker (never the paging loader — its flow never emits for null id).
                state.selectedFormId == null -> {
                    if (state.loading && state.formsToSelect.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.ink) }
                    } else {
                        FormSelectionList(state.formsToSelect) { viewModel.selectForm(it.id, it.title) }
                    }
                }
                lazyPagingItems.loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount == 0 -> {
                    ResponsesShimmer()
                }
                lazyPagingItems.itemCount == 0 && lazyPagingItems.loadState.refresh is LoadState.NotLoading -> {
                    EmptyResponses(Modifier.fillMaxSize())
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item {
                            StatRow(total = state.totalCount, fields = state.fields.size, live = state.formTitle.isNotBlank())
                            Spacer(Modifier.height(14.dp))
                        }
                        item {
                            InsightCard(
                                loading = state.loadingInsights,
                                summary = state.insightsSummary,
                                error = state.insightsError,
                                onGenerate = { viewModel.loadInsights() },
                            )
                            Spacer(Modifier.height(14.dp))
                        }
                        item {
                            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
                                SectionLabel("Responses", modifier = Modifier.weight(1f))
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                            Spacer(Modifier.height(8.dp))
                        }

                        items(
                            count = lazyPagingItems.itemCount,
                            key = lazyPagingItems.itemKey { it.id },
                            contentType = lazyPagingItems.itemContentType { "response" },
                        ) { index ->
                            lazyPagingItems[index]?.let { resp ->
                                ResponseRow(resp = resp, index = index, fields = state.fields, onClick = {
                                    viewModel.selectResponse(resp); onViewDetail()
                                })
                            }
                        }

                        if (lazyPagingItems.loadState.append is LoadState.Loading) {
                            item { Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = c.ink) } }
                        }
                        if (lazyPagingItems.loadState.refresh is LoadState.Error) {
                            item { ErrorItem((lazyPagingItems.loadState.refresh as LoadState.Error).error.message ?: "Failed to load") { lazyPagingItems.retry() } }
                        }
                        if (lazyPagingItems.loadState.append is LoadState.Error) {
                            item { ErrorItem((lazyPagingItems.loadState.append as LoadState.Error).error.message ?: "Failed to load more") { lazyPagingItems.retry() } }
                        }
                    }
                }
            }

            state.error?.let { msg ->
                Box(Modifier.align(Alignment.BottomCenter).padding(16.dp).clip(RoundedCornerShape(14.dp)).background(c.accentSoft).padding(12.dp)) {
                    Text(msg, color = c.accentDeep, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun StatRow(total: Int, fields: Int, live: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(total.toString(), "Responses", Modifier.weight(1f))
        StatTile(fields.toString(), "Fields", Modifier.weight(1f))
        StatTile(if (live) "Live" else "Draft", "Status", Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(num: String, label: String, modifier: Modifier = Modifier) {
    val c = SuvTheme.colors
    SuvCard(radius = 14, contentPadding = PaddingValues(12.dp), modifier = modifier) {
        Column {
            Text(num, fontFamily = Fraunces, fontSize = 26.sp, letterSpacing = (-0.8).sp, color = c.ink, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Text(label.uppercase(), fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Medium, color = c.muted)
        }
    }
}

@Composable
private fun InsightCard(loading: Boolean, summary: String?, error: String?, onGenerate: () -> Unit) {
    val c = SuvTheme.colors
    SuvCard(radius = 18, border = false, container = c.feature, contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.clickable(enabled = summary == null && !loading, onClick = onGenerate)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(100.dp)).background(c.accent))
                Spacer(Modifier.width(8.dp))
                Text("AI SUMMARY", fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Medium, color = c.onFeature.copy(alpha = 0.55f))
            }
            Spacer(Modifier.height(10.dp))
            when {
                loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = c.onFeature)
                    Spacer(Modifier.width(12.dp))
                    Text("Analyzing responses…", color = c.onFeature.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
                error != null -> Text(error, color = c.accent, style = MaterialTheme.typography.bodyMedium)
                summary != null -> Text(
                    summary,
                    fontFamily = Fraunces,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = c.onFeature,
                )
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Generate a quick overview of all responses", color = c.onFeature.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ResponseRow(resp: ResponseItemDto, index: Int, fields: List<FieldDto>, onClick: () -> Unit) {
    val c = SuvTheme.colors
    val fmt = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    val firstAnswer = remember(resp, fields) {
        val f = fields.firstOrNull()
        val raw = if (f != null) resp.answers[f.id] else resp.answers.values.firstOrNull()
        raw?.let { renderValue(it) }.orEmpty()
    }
    val avatarLetter = firstAnswer.firstOrNull()?.uppercase()?.takeIf { it.first().isLetter() } ?: "R"
    val (avBg, avFg) = when (index % 3) {
        0 -> c.accentSoft to c.accentDeep
        1 -> c.okSoft to c.ok
        else -> c.warnSoft to c.warn
    }

    Box(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)) {
        SuvCard(radius = 14, contentPadding = PaddingValues(12.dp), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(100.dp)).background(avBg), contentAlignment = Alignment.Center) {
                    Text(avatarLetter, fontFamily = Fraunces, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = avFg)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        firstAnswer.ifBlank { "Response #${index + 1}" },
                        fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = c.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    val preview = if (resp.calculated.isNotEmpty())
                        resp.calculated.entries.take(2).joinToString(" · ") { "${it.key} ${it.value}" }
                    else resp.answers.size.let { "$it ${if (it == 1) "answer" else "answers"}" }
                    Text(preview, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                MonoMeta(fmt.format(Date(resp.submittedAt)), color = c.muted, size = 10)
            }
        }
    }
}

@Composable
private fun ErrorItem(msg: String, onRetry: () -> Unit) {
    val c = SuvTheme.colors
    SuvCard(radius = 14, border = false, container = c.accentSoft, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(msg, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = c.accentDeep)
            TextButton(onClick = onRetry) { Text("Retry", color = c.accent, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ResponsesShimmer() {
    val c = SuvTheme.colors
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(0f, 1000f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "shimmer")
    val brush = Brush.linearGradient(listOf(c.card, c.paper2, c.card), start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset(x, x))
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(14.dp)).background(brush)) }
        items(5) { Box(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(14.dp)).background(brush)) }
    }
}

@Composable
private fun EmptyResponses(modifier: Modifier = Modifier) {
    val c = SuvTheme.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        GlyphIcon("R", size = 48, radius = 14)
        Spacer(Modifier.height(16.dp))
        Text("No responses yet", fontFamily = Fraunces, fontSize = 22.sp, color = c.ink)
        Spacer(Modifier.height(6.dp))
        Text("Share your form's link to start collecting responses.", style = MaterialTheme.typography.bodySmall, color = c.muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FormSelectionList(forms: List<FormSummaryDto>, onSelect: (FormSummaryDto) -> Unit) {
    val c = SuvTheme.colors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column {
                Text("Responses", fontFamily = Fraunces, fontSize = 30.sp, letterSpacing = (-0.7).sp, color = c.ink)
                Spacer(Modifier.height(4.dp))
                Text("Select a form to view its responses.", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                Spacer(Modifier.height(10.dp))
            }
        }
        items(forms, key = { it.id }) { form ->
            Box(Modifier.clip(RoundedCornerShape(18.dp)).clickable { onSelect(form) }) {
                SuvCard(radius = 18, contentPadding = PaddingValues(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlyphIcon(form.title.firstOrNull()?.uppercase() ?: "F")
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(form.title, fontFamily = MaterialTheme.typography.titleMedium.fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (form.published == 1) {
                                Spacer(Modifier.height(4.dp))
                                MonoMeta("Published", color = c.ok)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun renderValue(value: JsonElement): String = when (value) {
    is JsonPrimitive -> runCatching { value.content }.getOrElse { value.toString() }
    is JsonArray -> value.jsonArray.joinToString(", ") { runCatching { it.jsonPrimitive.content }.getOrDefault(it.toString()) }
    else -> value.toString()
}
