package com.shrijilala.tapcounter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shrijilala.tapcounter.data.CountSession
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(sessions: List<CountSession>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No saved counts yet. They show up here after a Reset.")
            }
        } else {
            val formatter = remember(sessions) { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(sessions) { session ->
                    ListItem(
                        headlineContent = { Text(session.finalCount.toString(), fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(formatter.format(Date(session.savedAtEpochMillis))) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
