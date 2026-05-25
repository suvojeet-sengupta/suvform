package com.suvojeetsengupta.suvform.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeetsengupta.suvform.data.remote.FieldDto
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
fun AdminFormResponsesScreen(
    onBack: () -> Unit,
    viewModel: AdminFormResponsesViewModel = hiltViewModel(),
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val fields by viewModel.fields.collectAsStateWithLifecycle()
    val responses by viewModel.responses.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode = selectedIds.isNotEmpty()
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val fmt = remember { SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Filled.Delete, "Delete")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Responses", maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, "More")
                            }
                            androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Delete all responses", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { menuOpen = false; showDeleteAllConfirm = true }
                                )
                            }
                        }
                    }
                )
            }
        },
    ) { padding ->
        if (showDeleteAllConfirm) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteAllConfirm = false },
                title = { Text("Delete All Responses?") },
                text = { Text("This will permanently remove ALL responses for this form from ALL users. This action cannot be undone.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showDeleteAllConfirm = false
                        viewModel.deleteAll()
                    }) {
                        Text("Delete All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showDeleteAllConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        if (loading && responses.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                if (title.isNotBlank()) Text(title, style = MaterialTheme.typography.titleLarge)
                Text("${responses.total} responses", style = MaterialTheme.typography.bodyMedium)
            }

            error?.let {
                item { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
            }

            if (responses.items.isEmpty() && !loading) {
                item { Text("No responses yet.", style = MaterialTheme.typography.bodyMedium) }
            }

            items(responses.items, key = { it.id }) { resp ->
                val isSelected = selectedIds.contains(resp.id)
                ResponseCard(
                    response = resp,
                    fields = fields,
                    submittedAt = fmt.format(Date(resp.submittedAt)),
                    isExpanded = expanded[resp.id] == true,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    onToggle = {
                        if (isSelectionMode) {
                            viewModel.toggleSelection(resp.id)
                        } else {
                            expanded[resp.id] = !(expanded[resp.id] ?: false)
                        }
                    },
                    onLongClick = {
                        if (!isSelectionMode) {
                            viewModel.toggleSelection(resp.id)
                        }
                    }
                )
            }

            if (responses.hasMore) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        if (responses.loadingMore) {
                            CircularProgressIndicator(modifier = Modifier.height(28.dp))
                        } else {
                            OutlinedButton(onClick = { viewModel.loadMore() }) { Text("Load more") }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ResponseCard(
    response: ResponseItemDto,
    fields: List<FieldDto>,
    submittedAt: String,
    isExpanded: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle, onLongClick = onLongClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode) {
                    androidx.compose.material3.Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(submittedAt, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (!isSelectionMode) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                    )
                }
            }
            AnimatedVisibility(visible = isExpanded && !isSelectionMode) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    fields.forEach { field ->
                        DetailItem(field.label.ifBlank { field.id }, displayValue(response.answers[field.id]))
                        Spacer(Modifier.height(10.dp))
                    }
                    if (response.calculated.isNotEmpty()) {
                        Text("Calculations", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        response.calculated.forEach { (k, v) ->
                            DetailItem(k, v.toString(), isCalc = true)
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String, isCalc: Boolean = false) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Text(
            value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyLarge,
            color = if (isCalc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isCalc) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private fun displayValue(value: JsonElement?): String = when (value) {
    null -> "—"
    is JsonPrimitive -> runCatching { value.content }.getOrDefault(value.toString())
    is JsonArray -> value.jsonArray.joinToString(", ") { runCatching { it.jsonPrimitive.content }.getOrDefault(it.toString()) }
    else -> value.toString()
}
