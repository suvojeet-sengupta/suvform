package com.suvojeetsengupta.suvform.ui.editor

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Editor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onPreview, enabled = draft.fields.isNotEmpty()) {
                        Icon(Icons.Default.Visibility, "Preview")
                    }
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = !saveState.saving && draft.fields.isNotEmpty(),
                    ) {
                        Text(if (saveState.saving) "Saving…" else "Save", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.addField() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, "Add field")
            }
        },
        bottomBar = {
            PublishBar(
                published = draft.published,
                hasRemoteId = draft.remoteId != null,
                shareUrl = draft.shareUrl,
                saving = saveState.saving,
                onPublish = {
                    if (draft.remoteId == null) viewModel.saveAndPublish()
                    else viewModel.publish()
                },
                onUnpublish = { viewModel.unpublish() },
                onCopy = {
                    draft.shareUrl?.let {
                        clipboard.setText(AnnotatedString(it))
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                FormHeaderCard(
                    title = draft.title,
                    description = draft.description,
                    onTitleChange = viewModel::setTitle,
                    onDescriptionChange = viewModel::setDescription,
                )
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        "Fields",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text("${draft.fields.size}", modifier = Modifier.padding(4.dp))
                    }
                }
            }

            itemsIndexed(draft.fields, key = { _, f -> f.id }) { index, field ->
                FieldEditorCard(
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
                item { EmptyFieldsState() }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun FormHeaderCard(
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Untitled Form", style = MaterialTheme.typography.headlineSmall) },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add a description...") },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
                minLines = 1,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
    }
}

@Composable
private fun FieldEditorCard(
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
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh 
                             else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 2.dp else 0.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(Modifier.padding(if (expanded) 20.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(field.type.emoji, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (field.label.isBlank()) "New Field" else field.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = field.type.display,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (field.required) {
                    Text(
                        "*", 
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TypeSelector(current = field.type, onSelect = onTypeChange)
                        
                        Row {
                            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                                Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = onMoveDown, enabled = !isLast) {
                                Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = field.label,
                        onValueChange = onLabelChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Question Label") },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )

                    if (field.type !in setOf(FieldType.SINGLE_CHOICE, FieldType.MULTI_CHOICE, FieldType.RATING, FieldType.DATE)) {
                        OutlinedTextField(
                            value = field.placeholder.orEmpty(),
                            onValueChange = onPlaceholderChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Placeholder (optional)") },
                            shape = RoundedCornerShape(12.dp),
                        )
                    }

                    if (field.type.hasOptions) {
                        Text(
                            "Options",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        field.options.forEachIndexed { idx, opt ->
                            OptionItem(
                                value = opt,
                                onValueChange = { onOptionChange(idx, it) },
                                onRemove = { onOptionRemove(idx) }
                            )
                        }
                        TextButton(onClick = onAddOption) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Option")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = field.required, onCheckedChange = onRequiredChange)
                            Text("Required", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDuplicate) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = onDelete,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionItem(value: String, onValueChange: (String) -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.RadioButtonUnchecked, 
            null, 
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.width(12.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
            )
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TypeSelector(current: FieldType, onSelect: (FieldType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        InputChip(
            selected = true,
            onClick = { expanded = true },
            label = { Text(current.display) },
            leadingIcon = { Text(current.emoji) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
            shape = RoundedCornerShape(12.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FieldType.entries.forEach { t ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.emoji)
                            Spacer(Modifier.width(12.dp))
                            Text(t.display)
                        }
                    },
                    onClick = {
                        onSelect(t)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PublishBar(
    published: Boolean,
    hasRemoteId: Boolean,
    shareUrl: String?,
    saving: Boolean,
    onPublish: () -> Unit,
    onUnpublish: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            if (published && shareUrl != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Form is Live",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            shareUrl,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, null)
                    }
                    Button(
                        onClick = onShare,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share")
                    }
                }
                TextButton(
                    onClick = onUnpublish,
                    modifier = Modifier.padding(top = 4.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Unpublish Form", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Button(
                    onClick = onPublish,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !saving,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Saving...")
                    } else {
                        Icon(Icons.Default.CloudUpload, null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (hasRemoteId) "Publish Changes" else "Save & Publish",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFieldsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.PostAdd,
            null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No fields added yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Tap + to add your first question",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
