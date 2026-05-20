package com.suvojeetsengupta.suvform.ui.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeetsengupta.suvform.data.remote.GeneratedFormDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    onBack: () -> Unit,
    viewModel: CreateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New form") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Describe what form you need", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "AI will build it for you. Example: \"Feedback form for coaching classes with rating and total students\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.prompt,
                onValueChange = viewModel::updatePrompt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                placeholder = { Text("e.g. Make a quotation form with item, qty, price and total") },
                enabled = !state.loading,
            )
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Language:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(12.dp))
                FilterChip(
                    selected = state.locale == "en",
                    onClick = { if (state.locale != "en") viewModel.toggleLocale() },
                    label = { Text("English") },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = state.locale == "hi",
                    onClick = { if (state.locale != "hi") viewModel.toggleLocale() },
                    label = { Text("हिन्दी") },
                )
            }

            Spacer(Modifier.height(16.dp))
            ElevatedButton(
                onClick = { viewModel.generate() },
                enabled = !state.loading && state.prompt.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Generating…")
                } else {
                    Text("✨ Generate with AI")
                }
            }

            state.error?.let { msg ->
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            state.result?.let { form ->
                Spacer(Modifier.height(20.dp))
                GeneratedFormPreview(form)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun GeneratedFormPreview(form: GeneratedFormDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(form.title, style = MaterialTheme.typography.titleLarge)
            form.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("Fields (${form.fields.size})", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            form.fields.forEachIndexed { i, f ->
                Text(
                    text = buildString {
                        append("${i + 1}. ${f.label}")
                        append("  [${f.type}")
                        if (f.required) append(" • required")
                        append("]")
                        if (f.options.isNotEmpty()) append("  options: ${f.options.joinToString(", ")}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }

            if (form.calculations.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Calculations (${form.calculations.size})", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                form.calculations.forEach { c ->
                    Text(
                        text = "• ${c.label} = ${c.expression}" + (c.format?.let { "  (${it})" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
