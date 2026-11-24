package com.shrijilala.tapcounter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shrijilala.tapcounter.library.LibraryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onLearn: (String) -> Unit,
    onPractice: () -> Unit
) {
    val grouped = LibraryRepository.docs.groupBy { it.section }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("पाठ संग्रह") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
            actions = { TextButton(onClick = onPractice) { Text("Pad Practice") } }
        )
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            grouped.forEach { (section, docs) ->
                val open = expanded[section] ?: true
                item(key = "h_$section") {
                    Row(
                        Modifier.fillMaxWidth().clickable { expanded[section] = !open }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(section, Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                    }
                    HorizontalDivider()
                }
                if (open) items(docs, key = { it.id }) { d ->
                    ListItem(
                        headlineContent = { Text(d.title) },
                        supportingContent = {
                            val n = d.pads.size
                            Text(if (n == 0) "पाठ जोड़ना बाकी" else "$n पद" + if (d.hasMeaning) " • अर्थ सहित" else "")
                        },
                        trailingContent = {
                            if (d.practice) IconButton(onClick = { onLearn(d.id) }) { Icon(Icons.Filled.FormatListNumbered, "Learn pad") }
                        },
                        modifier = Modifier.clickable { onOpen(d.id) }
                    )
                }
            }
        }
    }
}
