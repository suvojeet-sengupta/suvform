package com.suvojeetsengupta.suvform.ui.preview

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeetsengupta.suvform.R
import com.suvojeetsengupta.suvform.data.draft.CalculationEdit
import com.suvojeetsengupta.suvform.data.draft.FieldEdit
import com.suvojeetsengupta.suvform.data.draft.FieldType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    onBack: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val answers by viewModel.answers.collectAsStateWithLifecycle()
    val calculated by viewModel.calculated.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.clear() }) { Text("Reset") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Hero header
            item {
                Column {
                    Text(
                        draft.title.ifBlank { "Untitled form" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (draft.description.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            draft.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(draft.fields, key = { it.id }) { field ->
                FieldRenderer(
                    field = field,
                    answer = answers[field.id],
                    onTextChange = { viewModel.setText(field.id, it) },
                    onNumberChange = { viewModel.setNumber(field.id, it) },
                    onSingleChoice = { viewModel.setSingleChoice(field.id, it) },
                    onToggleMulti = { viewModel.toggleMultiChoice(field.id, it) },
                    onRatingChange = { viewModel.setRating(field.id, it) },
                    onDateChange = { viewModel.setDate(field.id, it) },
                )
            }

            if (draft.calculations.isNotEmpty()) {
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    Text(
                        "Calculations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(draft.calculations, key = { it.id }) { calc ->
                    CalculationCard(calc, calculated[calc.id] ?: 0.0)
                }
            }

            // Submit button (visual only — no real submission yet, that's Week 3)
            item {
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        "Submit",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Preview only — submissions happen once you publish the form.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldRenderer(
    field: FieldEdit,
    answer: Any?,
    onTextChange: (String) -> Unit,
    onNumberChange: (String) -> Unit,
    onSingleChoice: (String) -> Unit,
    onToggleMulti: (String) -> Unit,
    onRatingChange: (Int) -> Unit,
    onDateChange: (Long?) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            LabelRow(field)
            Spacer(Modifier.height(10.dp))
            when (field.type) {
                FieldType.SHORT_TEXT, FieldType.EMAIL, FieldType.PHONE ->
                    OutlinedTextField(
                        value = (answer as? String).orEmpty(),
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { field.placeholder?.let { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = when (field.type) {
                                FieldType.EMAIL -> KeyboardType.Email
                                FieldType.PHONE -> KeyboardType.Phone
                                else -> KeyboardType.Text
                            },
                        ),
                    )

                FieldType.LONG_TEXT ->
                    OutlinedTextField(
                        value = (answer as? String).orEmpty(),
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { field.placeholder?.let { Text(it) } },
                        minLines = 3,
                        maxLines = 6,
                    )

                FieldType.NUMBER ->
                    OutlinedTextField(
                        value = (answer as? String).orEmpty(),
                        onValueChange = { input ->
                            // Allow digits, optional minus + single decimal point.
                            if (input.isEmpty() || input.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                                onNumberChange(input)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { field.placeholder?.let { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )

                FieldType.SINGLE_CHOICE -> SingleChoiceList(
                    options = field.options,
                    selected = (answer as? String).orEmpty(),
                    onSelect = onSingleChoice,
                )

                FieldType.MULTI_CHOICE -> MultiChoiceList(
                    options = field.options,
                    selected = (answer as? Set<*>)?.filterIsInstance<String>()?.toSet().orEmpty(),
                    onToggle = onToggleMulti,
                )

                FieldType.RATING -> RatingRow(
                    rating = (answer as? Int) ?: 0,
                    onChange = onRatingChange,
                )

                FieldType.DATE -> DateField(
                    millis = answer as? Long,
                    onChange = onDateChange,
                )
            }
        }
    }
}

@Composable
private fun LabelRow(field: FieldEdit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            field.label.ifBlank { "Untitled field" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (field.required) {
            Text(
                " *",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SingleChoiceList(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column {
        options.forEach { opt ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = opt == selected, onClick = { onSelect(opt) })
                Spacer(Modifier.width(8.dp))
                Text(opt)
            }
        }
    }
}

@Composable
private fun MultiChoiceList(options: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    Column {
        options.forEach { opt ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = opt in selected, onCheckedChange = { onToggle(opt) })
                Spacer(Modifier.width(8.dp))
                Text(opt)
            }
        }
    }
}

@Composable
private fun RatingRow(rating: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            IconButton(onClick = { onChange(if (rating == i) 0 else i) }) {
                Icon(
                    painter = painterResource(if (i <= rating) R.drawable.ic_star else R.drawable.ic_star_outline),
                    contentDescription = "$i star",
                    tint = if (i <= rating)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (rating == 0) "Tap to rate" else "$rating / 5",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(millis: Long?, onChange: (Long?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(painterResource(R.drawable.ic_calendar_month), null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (millis != null) fmt.format(Date(millis)) else "Select date")
    }
    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(state.selectedDateMillis)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun CalculationCard(calc: CalculationEdit, value: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    calc.label.ifBlank { "Calculation" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    calc.expression,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
            Text(
                formatValue(value, calc.format),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 22.sp,
            )
        }
    }
}

private fun formatValue(v: Double, format: String?): String = when (format) {
    "percent" -> "${"%.1f".format(v)}%"
    "currency" -> "₹${"%,.2f".format(v)}"
    else -> if (v == v.toLong().toDouble()) v.toLong().toString() else "%.2f".format(v)
}
