package com.suvojeetsengupta.suvform.ui.admin

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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeetsengupta.suvform.data.remote.AdminFormDto
import com.suvojeetsengupta.suvform.data.remote.AdminUserDto

@Composable
fun AdminScreen(
    onOpenUser: (String) -> Unit,
    onOpenForm: (String) -> Unit,
    viewModel: AdminViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val forms by viewModel.forms.collectAsStateWithLifecycle()
    val admins by viewModel.admins.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val revoked by viewModel.revoked.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    var newAdminEmail by remember { mutableStateOf("") }

    Scaffold { padding ->
        if (loading && stats == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (revoked) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    error ?: "Your admin access has been revoked.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Admin Dashboard", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
            }

            // Stats
            stats?.let { s ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard("Users", s.totalUsers, Modifier.weight(1f))
                        StatCard("Forms", s.totalForms, Modifier.weight(1f))
                        StatCard("Responses", s.totalResponses, Modifier.weight(1f))
                        StatCard("Admins", s.totalAdmins, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Banners
            error?.let {
                item {
                    FeedbackBanner(it, isError = true) { viewModel.clearError() }
                }
            }
            message?.let {
                item {
                    FeedbackBanner(it, isError = false) { viewModel.clearMessage() }
                }
            }

            // Manage admins
            item {
                SectionHeader("Manage Admins")
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newAdminEmail,
                        onValueChange = { newAdminEmail = it },
                        label = { Text("Add admin by email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            viewModel.addAdminByEmail(newAdminEmail)
                            newAdminEmail = ""
                        },
                        enabled = newAdminEmail.isNotBlank(),
                        modifier = Modifier.padding(start = 8.dp),
                    ) { Text("Add") }
                }
                Spacer(Modifier.height(8.dp))
            }

            items(admins, key = { it.uid }) { admin ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(admin.email ?: admin.uid, style = MaterialTheme.typography.bodyLarge)
                            Text("Role: ${admin.role}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (admin.role != "owner") {
                            TextButton(onClick = { viewModel.removeAdmin(admin.uid) }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Text("Owner", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Users
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("Users (${users.total})")
            }
            items(users.items, key = { "u_" + it.uid }) { u ->
                UserRow(u) { onOpenUser(u.uid) }
            }
            if (users.hasMore) {
                item { LoadMoreRow(users.loadingMore) { viewModel.loadMoreUsers() } }
            }

            // Forms
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("Forms (${forms.total})")
            }
            items(forms.items, key = { "f_" + it.id }) { f ->
                FormRow(f) { onOpenForm(f.id) }
            }
            if (forms.hasMore) {
                item { LoadMoreRow(forms.loadingMore) { viewModel.loadMoreForms() } }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun StatCard(title: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 4.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge)
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun UserRow(u: AdminUserDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(u.displayName ?: u.email ?: u.uid, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (u.email != null && u.displayName != null) {
                    Text(u.email, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open")
        }
    }
}

@Composable
private fun FormRow(f: AdminFormDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(f.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "by ${f.ownerName ?: f.ownerEmail ?: f.ownerUid}" + if (f.published == 1) " • published" else "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open")
        }
    }
}

@Composable
private fun LoadMoreRow(loading: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.height(28.dp))
        } else {
            OutlinedButton(onClick = onClick) { Text("Load more") }
        }
    }
}

@Composable
private fun FeedbackBanner(text: String, isError: Boolean, onDismiss: () -> Unit) {
    val container = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val content = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Card(colors = CardDefaults.cardColors(containerColor = container), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = content, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onDismiss) { Text("Dismiss", color = content) }
        }
    }
}
