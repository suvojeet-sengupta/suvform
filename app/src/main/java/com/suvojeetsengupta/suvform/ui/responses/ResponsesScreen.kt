package com.suvojeetsengupta.suvform.ui.responses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeetsengupta.suvform.data.remote.ResponseItemDto
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
    viewModel: ResponsesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(enabled = true) {
        if (state.selectedFormId != null) {
            viewModel.clearSelection()
        } else {
            onBack()
        }
    }

    val showInsights = state.insightsSummary != null || state.loadingInsights || state.insightsError != null
    var insightsOpen by remember { mutableStateOf(false) }
    if (showInsights && !insightsOpen) insightsOpen = true
    var exportMenuOpen by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.formTitle, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(
                            "${state.responses.size} responses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.selectedFormId != null) {
                            viewModel.clearSelection()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { exportMenuOpen = true },
                            enabled = state.responses.isNotEmpty() && !state.exporting,
                        ) {
                            if (state.exporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Filled.IosShare, "Export")
                            }
                        }
                        DropdownMenu(
                            expanded = exportMenuOpen,
                            onDismissRequest = { exportMenuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export as CSV") },
                                leadingIcon = { Icon(Icons.Filled.TableChart, null) },
                                onClick = {
                                    exportMenuOpen = false
                                    viewModel.exportCsv(context)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                leadingIcon = { Icon(Icons.Filled.PictureAsPdf, null) },
                                onClick = {
                                    exportMenuOpen = false
                                    viewModel.exportPdf(context)
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = { viewModel.refresh() },
            state = refreshState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                state.formsToSelect.isNotEmpty() -> {
                    FormSelectionList(
                        forms = state.formsToSelect,
                        onSelect = { viewModel.selectForm(it.id, it.title) }
                    )
                }
                state.responses.isEmpty() && !state.loading -> {
                    EmptyResponses(modifier = Modifier.fillMaxSize())
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item { InsightsCallout(onClick = { viewModel.loadInsights() }) }
                        items(state.responses, key = { it.id }) { resp ->
                            ResponseCard(
                                resp = resp,
                                fields = state.fields,
                                onClick = {
                                    viewModel.selectResponse(resp)
                                    onViewDetail()
                                }
                            )
                        }
                    }
                }
            }

            state.error?.let { msg ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (insightsOpen) {
        InsightsDialog(
            loading = state.loadingInsights,
            summary = state.insightsSummary,
            error = state.insightsError,
            onDismiss = {
                insightsOpen = false
                viewModel.dismissInsights()
            },
        )
    }
}

@Composable
private fun InsightsCallout(onClick: () -> Unit) {
    androidx.compose.material3.OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Summary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Get a quick overview of all responses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ResponseCard(
    resp: ResponseItemDto,
    fields: List<com.suvojeetsengupta.suvform.data.remote.FieldDto>,
    onClick: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                fmt.format(Date(resp.submittedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            
            // Show first few answers with labels
            val previewFields = fields.take(3)
            if (previewFields.isEmpty()) {
                // Fallback to raw IDs if fields are not loaded yet
                resp.answers.entries.take(3).forEach { (key, value) ->
                    AnswerRow(key, value)
                }
            } else {
                previewFields.forEach { field ->
                    val answer = resp.answers[field.id]
                    val label = field.label.ifBlank { field.id }
                    AnswerRow(label, answer ?: JsonPrimitive(""))
                }
            }
            
            if (fields.size > 3) {
                Text(
                    "+ ${fields.size - 3} more fields",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (resp.calculated.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Calculations",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                resp.calculated.forEach { (k, v) ->
                    AnswerRow(k, JsonPrimitive(v), isCalc = true)
                }
            }
        }
    }
}

@Composable
private fun AnswerRow(key: String, value: JsonElement, isCalc: Boolean = false) {
    val displayValue = remember(value) {
        when (value) {
            is JsonPrimitive -> value.contentOrNull() ?: value.toString()
            is JsonArray -> value.jsonArray.joinToString(", ") {
                runCatching { it.jsonPrimitive.content }.getOrDefault(it.toString())
            }
            else -> value.toString()
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Text(
            text = if (displayValue.isBlank()) "—" else displayValue,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCalc) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isCalc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun JsonPrimitive.contentOrNull(): String? =
    runCatching { content }.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InsightsDialog(
    loading: Boolean,
    summary: String?,
    error: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("AI insights", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            when {
                loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Analyzing responses…")
                }
                error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                summary != null -> Text(summary, style = MaterialTheme.typography.bodyMedium)
                else -> Text("Tap to generate insights.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun EmptyResponses(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Inbox,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No responses yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Share your form's link to start collecting responses.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FormSelectionList(
    forms: List<com.suvojeetsengupta.suvform.data.remote.FormSummaryDto>,
    onSelect: (com.suvojeetsengupta.suvform.data.remote.FormSummaryDto) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Select a form to view responses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(forms, key = { it.id }) { form ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                onClick = { onSelect(form) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Inbox,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        form.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
