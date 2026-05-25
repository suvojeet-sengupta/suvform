package com.suvojeetsengupta.suvform.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeetsengupta.suvform.R

/**
 * A destructive-action dialog with two explicit steps:
 *  1. A warning the admin must acknowledge ("Continue").
 *  2. A type-to-confirm step — the admin must type [confirmPhrase] exactly
 *     before the final destructive button is enabled.
 *
 * @param confirmPhrase the exact text the admin must type (e.g. the form title or user email).
 */
@Composable
fun TwoStepDeleteDialog(
    title: String,
    warning: String,
    confirmPhrase: String,
    confirmActionLabel: String,
    inProgress: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableIntStateOf(1) }
    var typed by remember { mutableStateOf("") }
    val matches = typed.trim().equals(confirmPhrase.trim(), ignoreCase = true)

    AlertDialog(
        onDismissRequest = { if (!inProgress) onDismiss() },
        icon = { Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(title) },
        text = {
            Column {
                if (step == 1) {
                    Text(warning, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This action is permanent and cannot be undone.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        "Final confirmation. Type the following to delete:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(confirmPhrase, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        singleLine = true,
                        label = { Text("Type to confirm") },
                        enabled = !inProgress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                TextButton(onClick = { step = 2 }) {
                    Text("Continue", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onConfirm, enabled = matches && !inProgress) {
                    Text(confirmActionLabel, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !inProgress) { Text("Cancel") }
        },
    )
}

