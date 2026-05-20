package com.suvojeetsengupta.suvform.ui.create

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    onBack: () -> Unit,
    onOpenEditor: () -> Unit,
    viewModel: CreateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigateToEditor) {
        if (state.navigateToEditor) {
            onOpenEditor()
            viewModel.onNavigated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create a form") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ----- Hero AI card -----
            AiPromptCard(
                prompt = state.prompt,
                locale = state.locale,
                loading = state.loading,
                onPromptChange = viewModel::updatePrompt,
                onLocaleChange = viewModel::setLocale,
                onGenerate = viewModel::generate,
            )

            // ----- Error -----
            state.error?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // ----- Divider with text -----
            DividerWithText("or build manually")

            // ----- Manual mode card -----
            ManualCard(onStartBlank = { viewModel.startBlank() })

            Spacer(Modifier.height(24.dp))
        }
    }
}

// --------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiPromptCard(
    prompt: String,
    locale: String,
    loading: Boolean,
    onPromptChange: (String) -> Unit,
    onLocaleChange: (String) -> Unit,
    onGenerate: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Section title — neutral, no gradient
            Column {
                Text(
                    "Describe your form",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "We'll draft the fields for you to refine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("e.g. Quotation form for interior design with item, qty, price and total") },
                enabled = !loading,
                shape = RoundedCornerShape(14.dp),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Language:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(10.dp))
                FilterChip(
                    selected = locale == "en",
                    onClick = { onLocaleChange("en") },
                    label = { Text("English") },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = locale == "hi",
                    onClick = { onLocaleChange("hi") },
                    label = { Text("हिन्दी") },
                )
            }

            ElevatedButton(
                onClick = onGenerate,
                enabled = !loading && prompt.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Generating…", fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Generate draft", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DividerWithText(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.weight(1f).height(1.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        ) {}
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.weight(1f).height(1.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        ) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualCard(onStartBlank: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onStartBlank,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                onClick = onStartBlank,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.Filled.Edit, null)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Start from scratch",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Build it yourself, field by field.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

