package com.shrijilala.tapcounter.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun ResetDialog(currentCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset counter?") },
        text = {
            Text("Current count is $currentCount. It will be saved to History with today's date, then the counter will go back to 0. This can't be undone from a stray tap or button — you have to confirm it here.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Save & reset") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
