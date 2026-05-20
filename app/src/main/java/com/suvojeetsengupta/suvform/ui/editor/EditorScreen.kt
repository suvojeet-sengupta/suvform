package com.suvojeetsengupta.suvform.ui.editor

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardOptions
import com.suvojeetsengupta.suvform.data.draft.FieldEdit
import com.suvojeetsengupta.suvform.data.draft.FieldType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    onPreview: () -> Unit = {},
    onSaved: () -> Unit = {},
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(saveState.saved) {
        if (saveState.saved) {
            snackbar.showSnackbar("Form saved")
            viewModel.consumeSaved()
            onSaved()
        }
    }
    LaunchedEffect(saveState.published) {
        if (saveState.published) {
            snackbar.showSnackbar("Published — link is ready to share")
            viewModel.consumeSaved()
        }
    }
    LaunchedEffect(saveState.unpublished) {
        if (saveState.unpublished) {
            snackbar.showSnackbar("Form unpublished")
            viewModel.consumeSaved()
        }
    }
    LaunchedEffect(saveState.error) {
        saveState.error?.let { snackbar.showSnackbar(it) }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Edit form") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onPreview, enabled = draft.fields.isNotEmpty()) {
                        Text("Preview")
                    }
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = !saveState.saving && draft.fields.isNotEmpty(),
                    ) {
                        Text(if (saveState.saving) "Saving…" else "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.addField() },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add field") },
            )
        },
        bottomBar = {
            PublishBar(
                published = draft.published,
                hasRemoteId = draft.remoteId != null,
                shareUrl = draft.shareUrl,
                onPublish = { viewModel.publish() },
                onUnpublish = { viewModel.unpublish() },
                onCopy = {
                    draft.shareUrl?.let {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(it))
                    }
                },
                onShare = {
                    val link = draft.shareUrl ?: return@PublishBar
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, draft.title)
                        putExtra(android.content.Intent.EXTRA_TEXT, "${draft.title}\n$link")
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share form"))
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ----- Form header (title + description) -----
            item {
                FormHeader(
                    title = draft.title,
                    description = draft.description,
                    onTitleChange = viewModel::setTitle,
                    onDescriptionChange = viewModel::setDescription,
                )
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Fields",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(8.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text("${draft.fields.size}") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
                }
            }

            // ----- Field cards -----
            itemsIndexed(draft.fields, key = { _, f -> f.id }) { index, field ->
                FieldCard(
                    field = field,
                    isFirst = index == 0,
                    isLast = index == draft.fields.lastIndex,
                    onLabelChange = { viewModel.setFieldLabel(index, it) },
                    onTypeChange = { viewModel.setFieldType(index, it) },
                    onRequiredChange = { viewModel.setFieldRequired(index, it) },
                    onPlaceholderChange = { viewModel.setPlaceholder(index, it) },
                    onMoveUp = { viewModel.moveUp(index) },
                    onMoveDown = { viewModel.moveDown(index) },
                    onDuplicate = { viewModel.duplicateField(index) },
                    onDelete = { viewModel.deleteField(index) },
                    onAddOption = { viewModel.addOption(index) },
                    onOptionChange = { i, v -> viewModel.setOption(index, i, v) },
                    onOptionRemove = { i -> viewModel.removeOption(index, i) },
                )
            }

            if (draft.fields.isEmpty()) {
                item { EmptyFieldsHint() }
            }
        }
    }
}

// ----------------------- Form header -----------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormHeader(
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Form title") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description (optional)") },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
    }
}

// ----------------------- Field card -----------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldCard(
    field: FieldEdit,
    isFirst: Boolean,
    isLast: Boolean,
    onLabelChange: (String) -> Unit,
    onTypeChange: (FieldType) -> Unit,
    onRequiredChange: (Boolean) -> Unit,
    onPlaceholderChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onAddOption: () -> Unit,
    onOptionChange: (Int, String) -> Unit,
    onOptionRemove: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Top action row: type chip + reorder + duplicate + delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypePicker(current = field.type, onChange = onTypeChange)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f))
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Filled.ArrowUpward, "Move up", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Filled.ArrowDownward, "Move down", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Filled.ContentCopy, "Duplicate", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        "Delete",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            OutlinedTextField(
                value = field.label,
                onValueChange = onLabelChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Label") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            if (field.type !in setOf(FieldType.SINGLE_CHOICE, FieldType.MULTI_CHOICE, FieldType.RATING, FieldType.DATE)) {
                OutlinedTextField(
                    value = field.placeholder.orEmpty(),
                    onValueChange = onPlaceholderChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Placeholder (optional)") },
                    singleLine = true,
                )
            }

            // Required toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Required", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Switch(checked = field.required, onCheckedChange = onRequiredChange)
            }

            // Options for choice types
            if (field.type.hasOptions) {
                HorizontalDivider()
                Text(
                    "Options",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                field.options.forEachIndexed { idx, opt ->
                    OptionRow(
                        value = opt,
                        onValueChange = { onOptionChange(idx, it) },
                        onRemove = { onOptionRemove(idx) },
                    )
                }
                TextButton(onClick = onAddOption) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add option")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionRow(value: String, onValueChange: (String) -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, "Remove option", modifier = Modifier.size(18.dp))
        }
    }
}

// ----------------------- Type picker -----------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypePicker(current: FieldType, onChange: (FieldType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ElevatedAssistChip(
            onClick = { expanded = true },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(current.emoji, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(current.display, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Filled.ExpandMore, null, modifier = Modifier.size(16.dp))
                }
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FieldType.entries.forEach { t ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.emoji, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(t.display)
                        }
                    },
                    onClick = { onChange(t); expanded = false },
                )
            }
        }
    }
}

// ----------------------- Publish bar -----------------------

@Composable
private fun PublishBar(
    published: Boolean,
    hasRemoteId: Boolean,
    shareUrl: String?,
    onPublish: () -> Unit,
    onUnpublish: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (published && shareUrl != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Live link",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            shareUrl,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    androidx.compose.material3.OutlinedButton(onClick = onCopy) { Text("Copy") }
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.material3.FilledTonalButton(onClick = onShare) { Text("Share") }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    androidx.compose.material3.TextButton(onClick = onUnpublish) {
                        Text("Unpublish", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                androidx.compose.material3.FilledTonalButton(
                    onClick = onPublish,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasRemoteId,
                ) {
                    Text(
                        if (hasRemoteId) "Publish & share" else "Save first to publish",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ----------------------- Empty state -----------------------

@Composable
private fun EmptyFieldsHint() {
    Surface(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "No fields yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap “Add field” to start building.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
