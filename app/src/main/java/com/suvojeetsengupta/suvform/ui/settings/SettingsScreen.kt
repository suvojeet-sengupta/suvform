package com.suvojeetsengupta.suvform.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.suvojeetsengupta.suvform.BuildConfig
import com.suvojeetsengupta.suvform.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = { Text("You'll need to sign in again to access your forms.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    viewModel.signOut(context) { onSignedOut() }
                }) {
                    Text("Sign out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showApiKeyDialog) {
        var tempKey by remember { mutableStateOf(apiKey) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Gemini API Key") },
            text = {
                Column {
                    Text("Optional. Use your own Google AI Studio key for AI generation and insights. Leave blank to use the built-in key.")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveApiKey(tempKey)
                    showApiKeyDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About SuvForm") },
            text = {
                Column {
                    Text(
                        "SuvForm is a personal project born out of a desire for a clean, efficient, " +
                        "and mobile-first form management tool. I wanted to build something that " +
                        "combines the simplicity of a notepad with the power of a professional data " +
                        "collection platform."
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Whether you're conducting a quick poll, managing inventory, or gathering " +
                        "feedback, SuvForm leverages Gemini AI to help you build forms instantly and " +
                        "manages your data securely, even offline."
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Made with passion by Suvojeet Sengupta.", fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (user?.photoUrl != null) {
                        AsyncImage(
                            model = user?.photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(Modifier.width(20.dp))
                    
                    Column {
                        Text(
                            text = user?.displayName ?: "Anonymous User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = user?.email ?: "No email",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "AI & Preferences",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Gemini API Key") },
                    supportingContent = { Text(if (apiKey.isBlank()) "Not set" else "••••••••") },
                    leadingContent = { Icon(Icons.Default.Key, null) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { showApiKeyDialog = true },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Appearance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    ) {
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "App",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("About SuvForm") },
                        leadingContent = { Icon(Icons.Default.Info, null) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .clickable { showAboutDialog = true },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    ListItem(
                        headlineContent = { Text("Version") },
                        supportingContent = { Text(BuildConfig.VERSION_NAME) },
                        leadingContent = { Icon(Icons.Default.Info, null) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    ListItem(
                        headlineContent = { Text("Sign out", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { 
                            Icon(
                                Icons.AutoMirrored.Filled.Logout, 
                                null, 
                                tint = MaterialTheme.colorScheme.error 
                            ) 
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                            .clickable { showSignOutDialog = true },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }
        }
    }
}
