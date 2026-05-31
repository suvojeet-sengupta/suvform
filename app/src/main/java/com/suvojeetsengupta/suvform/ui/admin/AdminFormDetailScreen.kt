package com.suvojeetsengupta.suvform.ui.admin

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeetsengupta.suvform.R
import com.suvojeetsengupta.suvform.data.remote.CalculationDto
import com.suvojeetsengupta.suvform.data.remote.FieldDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFormDetailScreen(
    onBack: () -> Unit,
    onViewResponses: (String) -> Unit,
    viewModel: AdminFormDetailViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val editMode by viewModel.editMode.collectAsStateWithLifecycle()
    val draftTitle by viewModel.draftTitle.collectAsStateWithLifecycle()
    val draftDescription by viewModel.draftDescription.collectAsStateWithLifecycle()
    val draftResponseLimit by viewModel.draftResponseLimit.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val deleting by viewModel.deleting.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()

    LaunchedEffect(deleted) { if (deleted) onBack() }

    var showConfirm by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val formToDelete = form
    if (showDelete && formToDelete != null) {
        TwoStepDeleteDialog(
            title = "Delete this form?",
            warning = "This will permanently delete \"${formToDelete.title}\" and all ${formToDelete.totalResponses} of its responses, " +
                "owned by ${formToDelete.ownerName ?: formToDelete.ownerEmail ?: formToDelete.ownerUid}.",
            confirmPhrase = formToDelete.title.ifBlank { "DELETE" },
            confirmActionLabel = "Delete form",
            inProgress = deleting,
            onConfirm = { viewModel.deleteForm() },
            onDismiss = { showDelete = false },
        )
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon = { Icon(painterResource(R.drawable.ic_warning), contentDescription = null) },
            title = { Text("Save changes to another user's form?") },
            text = {
                Text(
                    "You are about to modify a form that belongs to " +
                        "${form?.ownerName ?: form?.ownerEmail ?: "another user"}. " +
                        "This will overwrite their content and they will see the change. Proceed only if necessary.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    viewModel.saveEdit()
                }) { Text("Save anyway", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editMode) "Editing form" else "Form details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
                actions = {
                    if (form != null && !editMode) {
                        IconButton(onClick = { viewModel.enterEditMode() }) {
                            Icon(painterResource(R.drawable.ic_edit), contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(painterResource(R.drawable.ic_delete), contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (loading && form == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val f = form ?: run {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(error ?: "Form not found", color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Caution banner — always visible: admin is viewing someone else's data.
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(0.dp))
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(
                                "You are viewing another user's data",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Owner: ${f.ownerName ?: f.ownerEmail ?: f.ownerUid}",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            error?.let {
                item { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
            }
            message?.let {
                item { Text(it, color = MaterialTheme.colorScheme.primary) }
            }

            // Title / description — editable in edit mode.
            item {
                if (editMode) {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = viewModel::setTitle,
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = draftDescription,
                        onValueChange = viewModel::setDescription,
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = draftResponseLimit?.toString() ?: "",
                        onValueChange = { text ->
                            viewModel.setResponseLimit(text.toIntOrNull())
                        },
                        label = { Text("Response Limit") },
                        placeholder = { Text("Unlimited") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.cancelEdit() }, enabled = !saving) { Text("Cancel") }
                        TextButton(onClick = { showConfirm = true }, enabled = !saving) {
                            if (saving) CircularProgressIndicator(Modifier.height(20.dp)) else Text("Save")
                        }
                    }
                } else {
                    Text(f.title, style = MaterialTheme.typography.headlineSmall)
                    if (!f.description.isNullOrBlank()) {
                        Text(f.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    f.updatedAtStr?.let {
                        Text("Updated: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    if (f.responseLimit != null && f.responseLimit > 0) {
                        Text("Response limit: ${f.responseLimit}", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Response limit: Unlimited", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth().clickable { onViewResponses(f.id) },
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "View ${f.totalResponses} responses",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                painterResource(R.drawable.ic_keyboard_arrow_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Fields (read-only — admin edit is limited to title/description).
            item { Text("Fields (${f.fields.size})", style = MaterialTheme.typography.titleMedium) }
            items(f.fields, key = { "fld_" + it.id }) { field ->
                FieldRow(field)
            }

            if (f.calculations.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Calculations (${f.calculations.size})", style = MaterialTheme.typography.titleMedium)
                }
                items(f.calculations, key = { "calc_" + it.id }) { calc ->
                    CalcRow(calc)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun FieldRow(field: FieldDto) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(field.label.ifBlank { "(no label)" } + if (field.required) " *" else "", style = MaterialTheme.typography.bodyLarge)
            Text("Type: ${field.type}", style = MaterialTheme.typography.bodySmall)
            if (field.options.isNotEmpty()) {
                Text("Options: ${field.options.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CalcRow(calc: CalculationDto) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(calc.label, style = MaterialTheme.typography.bodyLarge)
            Text(calc.expression, style = MaterialTheme.typography.bodySmall)
        }
    }
}
