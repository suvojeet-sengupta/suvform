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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeetsengupta.suvform.data.remote.AdminFormDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserDetailScreen(
    onBack: () -> Unit,
    onOpenForm: (String) -> Unit,
    viewModel: AdminUserDetailViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val forms by viewModel.forms.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val deleting by viewModel.deleting.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()

    LaunchedEffect(deleted) { if (deleted) onBack() }

    var showDelete by remember { mutableStateOf(false) }
    val u = user

    if (showDelete && u != null) {
        TwoStepDeleteDialog(
            title = "Delete this user?",
            warning = "This will permanently delete ${u.displayName ?: u.email ?: u.uid} along with " +
                "their ${u.totalForms} forms and ${u.totalResponses} responses. They will be signed out everywhere.",
            confirmPhrase = u.email ?: u.uid,
            confirmActionLabel = "Delete user",
            inProgress = deleting,
            onConfirm = { viewModel.deleteUser() },
            onDismiss = { showDelete = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (u != null && u.role != "owner") {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete user", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (loading && user == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            error?.let {
                item {
                    Text("Error: $it", color = MaterialTheme.colorScheme.error)
                }
            }

            user?.let { u ->
                item {
                    Spacer(Modifier.height(8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(u.displayName ?: u.email ?: u.uid, style = MaterialTheme.typography.titleLarge)
                            u.email?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            Text("UID: ${u.uid}", style = MaterialTheme.typography.bodySmall)
                            u.createdAtStr?.let { Text("Joined: $it", style = MaterialTheme.typography.bodySmall) }
                            if (u.isAdmin) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (u.role == "owner") "Owner" else "Admin",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                MiniStat("Forms", u.totalForms, Modifier.weight(1f))
                                MiniStat("Published", u.publishedForms, Modifier.weight(1f))
                                MiniStat("Responses", u.totalResponses, Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Forms (${forms.total})", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (user != null && forms.items.isEmpty() && !loading) {
                item { Text("This user has no forms.", style = MaterialTheme.typography.bodyMedium) }
            }

            items(forms.items, key = { it.id }) { f ->
                FormCard(f) { onOpenForm(f.id) }
            }

            if (forms.hasMore) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        if (forms.loadingMore) {
                            CircularProgressIndicator(modifier = Modifier.height(28.dp))
                        } else {
                            OutlinedButton(onClick = { viewModel.loadMoreForms() }) { Text("Load more") }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(vertical = 12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FormCard(f: AdminFormDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(f.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (f.published == 1) "Published" else "Draft",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open")
        }
    }
}
