package com.suvojeetsengupta.suvform.ui.responses

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
fun ResponseDetailScreen(
    response: ResponseItemDto,
    fields: List<FieldDto>,
    onBack: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault()) }

    // Legacy fields: answers that are not in the current fields list
    val currentFieldIds = remember(fields) { fields.map { it.id }.toSet() }
    val legacyAnswers = remember(response.answers, currentFieldIds) {
        response.answers.filterKeys { it !in currentFieldIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Response Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Submitted on",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = fmt.format(Date(response.submittedAt)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(24.dp))
            }

            items(fields) { field ->
                val answer = response.answers[field.id]
                val label = field.label.ifBlank { field.id }
                
                DetailItem(label = label, value = answer)
                Spacer(Modifier.height(20.dp))
            }

            if (legacyAnswers.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Legacy Data (Previous Versions)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                items(legacyAnswers.toList()) { (id, answer) ->
                    DetailItem(label = id, value = answer)
                    Spacer(Modifier.height(20.dp))
                }
            }

            if (response.calculated.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Calculations",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                items(response.calculated.toList()) { (key, value) ->
                    DetailItem(label = key, value = JsonPrimitive(value), isCalc = true)
                    Spacer(Modifier.height(20.dp))
                }
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: JsonElement?, isCalc: Boolean = false) {
    val displayValue = remember(value) {
        when (value) {
            null -> "—"
            is JsonPrimitive -> value.contentOrNull() ?: value.toString()
            is JsonArray -> value.jsonArray.joinToString(", ") {
                runCatching { it.jsonPrimitive.content }.getOrDefault(it.toString())
            }
            else -> value.toString()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (displayValue.isBlank()) "—" else displayValue,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isCalc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isCalc) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun JsonPrimitive.contentOrNull(): String? =
    runCatching { content }.getOrNull()
