package com.suvojeetsengupta.suvform.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AdminScreen(
    viewModel: AdminViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsState()
    val users by viewModel.users.collectAsState()
    val forms by viewModel.forms.collectAsState()
    val admins by viewModel.admins.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Admin Dashboard", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            if (loading && stats == null) {
                CircularProgressIndicator()
                return@Column
            }

            // Stats Cards
            stats?.let { s ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Users", s.totalUsers)
                    StatCard("Forms", s.totalForms)
                    StatCard("Responses", s.totalResponses)
                    StatCard("Admins", s.totalAdmins)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Manage Admins Section
            Text("Manage Admins", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            var newAdminUid by remember { mutableStateOf("") }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newAdminUid,
                    onValueChange = { newAdminUid = it },
                    label = { Text("New Admin UID") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        viewModel.addAdmin(newAdminUid)
                        newAdminUid = ""
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Add")
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn {
                items(admins) { admin ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(admin.email ?: admin.uid, style = MaterialTheme.typography.bodyLarge)
                                Text("Role: ${admin.role}", style = MaterialTheme.typography.bodySmall)
                                Text("Added: ${java.text.SimpleDateFormat("dd MMM yyyy").format(java.util.Date(admin.addedAt))}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            if (admin.role != "owner") {
                                TextButton(onClick = { viewModel.removeAdmin(admin.uid) }) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            } else {
                                Text("Owner", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            if (error != null) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.clearError() }) { Text("Dismiss") }
            }

            if (message != null) {
                Text(message!!, color = MaterialTheme.colorScheme.primary)
                Button(onClick = { viewModel.clearMessage() }) { Text("OK") }
            }

            Spacer(Modifier.height(24.dp))
            Text("All Users (${users.size})", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(users) { u ->
                    Text("• ${u.displayName ?: u.email ?: u.uid}")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("All Forms (${forms.size})", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(forms) { f ->
                    Text("• ${f.title} (by ${f.ownerName ?: f.ownerUid})")
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: Int) {
    Card(modifier = Modifier.weight(1f)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall)
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}
