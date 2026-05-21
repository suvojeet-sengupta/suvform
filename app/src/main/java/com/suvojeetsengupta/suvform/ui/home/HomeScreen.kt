package com.suvojeetsengupta.suvform.ui.home

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeetsengupta.suvform.data.remote.FormSummaryDto
import com.suvojeetsengupta.suvform.ui.components.ChipVariant
import com.suvojeetsengupta.suvform.ui.components.GlyphIcon
import com.suvojeetsengupta.suvform.ui.components.MonoMeta
import com.suvojeetsengupta.suvform.ui.components.SuvCard
import com.suvojeetsengupta.suvform.ui.components.SuvChip
import com.suvojeetsengupta.suvform.ui.theme.Accent
import com.suvojeetsengupta.suvform.ui.theme.AccentDeep
import com.suvojeetsengupta.suvform.ui.theme.AccentSoft
import com.suvojeetsengupta.suvform.ui.theme.CardWhite
import com.suvojeetsengupta.suvform.ui.theme.Fraunces
import com.suvojeetsengupta.suvform.ui.theme.Ink
import com.suvojeetsengupta.suvform.ui.theme.Line
import com.suvojeetsengupta.suvform.ui.theme.Mono
import com.suvojeetsengupta.suvform.ui.theme.Muted
import com.suvojeetsengupta.suvform.ui.theme.Paper2
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class FormFilter(val label: String) { All("All"), Live("Live"), Drafts("Drafts") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSignedOut: () -> Unit,
    onCreateForm: () -> Unit,
    onOpenForm: () -> Unit,
    onViewResponses: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteId by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf(FormFilter.All) }
    val refreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) { viewModel.refresh(force = false) }

    showDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteId = null },
            title = { Text("Delete form?") },
            text = { Text("This form and all its responses will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = showDeleteId
                    showDeleteId = null
                    if (toDelete != null) viewModel.delete(toDelete)
                }) { Text("Delete", color = Accent) }
            },
            dismissButton = { TextButton(onClick = { showDeleteId = null }) { Text("Cancel") } },
        )
    }

    val visibleForms = remember(state.forms, filter) {
        when (filter) {
            FormFilter.All -> state.forms
            FormFilter.Live -> state.forms.filter { it.published == 1 }
            FormFilter.Drafts -> state.forms.filter { it.published != 1 }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        buildAnnotatedString {
                            append("SuvForm")
                            withStyle(SpanStyle(color = Accent)) { append(".") }
                        },
                        fontFamily = Fraunces,
                        fontSize = 24.sp,
                        letterSpacing = (-0.4).sp,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateForm,
                containerColor = Ink,
                contentColor = CardWhite,
                shape = RoundedCornerShape(100.dp),
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New form", fontWeight = FontWeight.SemiBold) },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = { viewModel.refresh(force = true) },
            state = refreshState,
            modifier = Modifier.padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) {
                // Heading row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text("Your forms", fontFamily = Fraunces, fontSize = 30.sp, letterSpacing = (-0.7).sp, color = Ink, modifier = Modifier.weight(1f))
                        MonoMeta("${state.forms.size} ${if (state.forms.size == 1) "form" else "forms"}", color = Muted)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                    Spacer(Modifier.height(14.dp))
                }

                // Filter chips
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                        FormFilter.entries.forEach { f ->
                            Box(Modifier.clip(RoundedCornerShape(100.dp)).clickable { filter = f }) {
                                SuvChip(f.label, if (filter == f) ChipVariant.SolidInk else ChipVariant.Outline)
                            }
                        }
                    }
                }

                if (state.loading && state.forms.isEmpty()) {
                    items(5) { HomeShimmerItem(); Spacer(Modifier.height(10.dp)) }
                } else if (state.offline) {
                    item { Banner("Offline", "Showing your last synced forms.", AccentSoft, AccentDeep) }
                } else state.error?.let { msg ->
                    item { Banner("Error", msg, AccentSoft, AccentDeep) }
                }

                if (visibleForms.isEmpty() && !state.loading) {
                    item { EmptyFormsState() }
                } else {
                    itemsIndexed(visibleForms, key = { _, it -> it.id }) { index, form ->
                        FormListCard(
                            form = form,
                            featured = index == 0 && filter == FormFilter.All,
                            opening = state.openingFormId == form.id,
                            onClick = { viewModel.openForm(form.id) { onOpenForm() } },
                            onDelete = { showDeleteId = form.id },
                            onViewResponses = { viewModel.selectForResponses(form); onViewResponses() },
                            onShare = { context -> viewModel.shareForm(context, form) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                item { Spacer(Modifier.height(90.dp)) }
            }
        }
    }
}

@Composable
private fun Banner(tag: String, msg: String, bg: Color, fg: Color) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tag.uppercase(), fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg)
            Spacer(Modifier.width(10.dp))
            Text(msg, style = MaterialTheme.typography.bodySmall, color = fg)
        }
    }
}

@Composable
private fun HomeShimmerItem() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        0f, 1000f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "shimmer",
    )
    val brush = Brush.linearGradient(
        listOf(CardWhite, Paper2, CardWhite),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x, x),
    )
    Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(18.dp)).background(brush))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormListCard(
    form: FormSummaryDto,
    featured: Boolean,
    opening: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onViewResponses: () -> Unit,
    onShare: (Context) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("d MMM, yyyy", Locale.getDefault()) }
    val context = LocalContext.current

    val container = if (featured) Ink else CardWhite
    val titleColor = if (featured) CardWhite else Ink
    val metaColor = if (featured) CardWhite.copy(alpha = 0.65f) else Muted
    val iconBg = if (featured) CardWhite.copy(alpha = 0.12f) else Paper2
    val iconFg = if (featured) CardWhite else Ink

    Box(Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)) {
        SuvCard(border = !featured, container = container, contentPadding = PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphIcon(form.title.firstOrNull()?.uppercase() ?: "F", container = iconBg, content = iconFg)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            form.title,
                            fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (form.published == 1) {
                            Spacer(Modifier.width(8.dp))
                            SuvChip("LIVE", ChipVariant.Live)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    MonoMeta("Updated ${fmt.format(Date(form.updatedAt))}", color = metaColor)
                }
                if (opening) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = if (featured) CardWhite else Ink)
                } else {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, "More", tint = metaColor)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("View responses") }, leadingIcon = { Icon(Icons.Filled.Inbox, null) }, onClick = { menuOpen = false; onViewResponses() })
                            DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(Icons.Filled.Share, null) }, onClick = { menuOpen = false; onShare(context) })
                            DropdownMenuItem(text = { Text("Delete", color = Accent) }, leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Accent) }, onClick = { menuOpen = false; onDelete() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFormsState() {
    SuvCard(radius = 18, container = CardWhite, contentPadding = PaddingValues(vertical = 36.dp, horizontal = 24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            GlyphIcon("F", size = 48, radius = 14)
            Spacer(Modifier.height(14.dp))
            Text("No forms yet", fontFamily = Fraunces, fontSize = 22.sp, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("Tap “New form” to build your first one.", style = MaterialTheme.typography.bodySmall, color = Muted, textAlign = TextAlign.Center)
        }
    }
}
