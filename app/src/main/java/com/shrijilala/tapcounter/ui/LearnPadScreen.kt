package com.shrijilala.tapcounter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shrijilala.tapcounter.library.LibraryRepository

/** "Which pad is which number" — number + first line, searchable, tap to open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnPadScreen(docId: String, onBack: () -> Unit, onOpen: (String, Int) -> Unit) {
    var currentId by remember { mutableStateOf(docId) }
    var query by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val docs = LibraryRepository.docs
    val doc = docs.firstOrNull { it.id == currentId } ?: docs.first()
    val max = maxOf(doc.maxPadNo, 1)
    val rows = (1..max).map { n ->
        val first = doc.pads.firstOrNull { it.no == n }?.text?.get("hi")?.lineSequence()?.firstOrNull() ?: ""
        n to first
    }.filter { (n, first) ->
        query.isBlank() || n.toString() == query.trim() || first.contains(query.trim(), ignoreCase = true)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Learn Pad") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
            actions = {
                Box {
                    TextButton(onClick = { menu = true }) { Text(doc.title, maxLines = 1) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        docs.filter { it.practice }.forEach { d ->
                            DropdownMenuItem(text = { Text(d.title) }, onClick = { currentId = d.id; menu = false })
                        }
                    }
                }
            }
        )
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            OutlinedTextField(
                query, { query = it }, label = { Text("Search number or first words") },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(rows, key = { it.first }) { (n, first) ->
                    ListItem(
                        headlineContent = { Text(if (first.isBlank()) "— (खाली)" else first, maxLines = 1) },
                        leadingContent = { Text("$n", style = MaterialTheme.typography.titleLarge) },
                        modifier = Modifier.clickable { onOpen(doc.id, n) }
                    )
                }
            }
        }
    }
}
