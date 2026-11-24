package com.shrijilala.tapcounter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shrijilala.tapcounter.library.*

private const val COLS = 6

/**
 * Daily pad practice for Chaurashi / Radhasudhanidhi / Sevak Vani.
 * Pick pads (1–12, custom range, grid taps, whole row, whole column, or random),
 * then choose: repeat the same pads every day, OR move on without repeating in the series.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(onBack: () -> Unit, onStart: (String, List<Int>, Boolean) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val practiceDocs = LibraryRepository.docs.filter { it.practice }
    val saved = remember { PracticeStore.load(ctx) }

    var docId by remember { mutableStateOf(saved?.docId ?: practiceDocs.first().id) }
    val doc = practiceDocs.firstOrNull { it.id == docId } ?: practiceDocs.first()
    var totalStr by remember(docId) { mutableStateOf(maxOf(doc.maxPadNo, 12).toString()) }
    val total = (totalStr.toIntOrNull() ?: 12).coerceIn(1, 2000)

    var selected by remember { mutableStateOf(if (saved != null && saved.docId == docId) saved.series.toSet() else emptySet<Int>()) }
    var mode by remember { mutableIntStateOf(saved?.mode ?: 0) }
    var perDayStr by remember { mutableStateOf((saved?.perDay ?: 3).toString()) }
    var auto by remember { mutableStateOf(false) }
    var fromStr by remember { mutableStateOf("1") }
    var toStr by remember { mutableStateOf("12") }
    var row by remember { mutableIntStateOf(1) }
    var col by remember { mutableIntStateOf(1) }
    var randomN by remember { mutableStateOf("5") }
    var plan by remember { mutableStateOf(saved) }
    var menu by remember { mutableStateOf(false) }

    val rows = (total + COLS - 1) / COLS

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Pad Practice") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
        )
    }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ----- today -----
            plan?.let { p ->
                val pd = LibraryRepository.doc(p.docId)
                val today = PracticeStore.todayPads(p)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("आज (Day ${PracticeStore.dayNumber(p)}) — ${pd?.title ?: ""}", style = MaterialTheme.typography.titleMedium)
                        if (today == null) Text("इस series के सभी पद पूरे हो गए 🙏 — नया plan बनाएँ।")
                        else if (today.isEmpty()) Text("कोई पद चुना नहीं गया।")
                        else {
                            Text("पद: " + today.joinToString(", "))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onStart(p.docId, today, false) }) { Text("Start") }
                                OutlinedButton(onClick = { onStart(p.docId, today, true) }) { Text("Start in stealth") }
                            }
                        }
                    }
                }
            }

            // ----- choose text -----
            Box {
                OutlinedButton(onClick = { menu = true }) { Text(doc.title) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    practiceDocs.forEach { d ->
                        DropdownMenuItem(text = { Text(d.title) }, onClick = {
                            docId = d.id; selected = emptySet(); menu = false
                        })
                    }
                }
            }
            OutlinedTextField(
                totalStr, { totalStr = it.filter(Char::isDigit) }, label = { Text("Total pads in this text") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )

            // ----- quick picks -----
            Text("Pads चुनें (${selected.size} चुने)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { selected = (1..minOf(12, total)).toSet() }, label = { Text("1–12") })
                AssistChip(onClick = { selected = (1..total).toSet() }, label = { Text("All") })
                AssistChip(onClick = { selected = emptySet() }, label = { Text("Clear") })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(fromStr, { fromStr = it.filter(Char::isDigit) }, label = { Text("From") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(toStr, { toStr = it.filter(Char::isDigit) }, label = { Text("To") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(onClick = {
                    val a = (fromStr.toIntOrNull() ?: 1).coerceAtLeast(1)
                    val b = (toStr.toIntOrNull() ?: a).coerceAtMost(total)
                    if (a <= b) selected = selected + (a..b).toSet()
                }) { Text("Add") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Row")
                TextButton(onClick = { row = (row - 1).coerceAtLeast(1) }) { Text("−") }
                Text("$row")
                TextButton(onClick = { row = (row + 1).coerceAtMost(rows) }) { Text("+") }
                Button(onClick = {
                    val s = (row - 1) * COLS + 1
                    selected = selected + (s..minOf(s + COLS - 1, total)).toSet()
                }) { Text("Select row") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Col")
                TextButton(onClick = { col = (col - 1).coerceAtLeast(1) }) { Text("−") }
                Text("$col")
                TextButton(onClick = { col = (col + 1).coerceAtMost(COLS) }) { Text("+") }
                Button(onClick = { selected = selected + (1..total).filter { (it - 1) % COLS == col - 1 }.toSet() }) { Text("Select column") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(randomN, { randomN = it.filter(Char::isDigit) }, label = { Text("Random count") }, singleLine = true, modifier = Modifier.weight(1f))
                Button(onClick = {
                    val n = (randomN.toIntOrNull() ?: 5).coerceIn(1, total)
                    selected = (1..total).shuffled().take(n).toSet()
                }) { Text("Pick random") }
            }

            // ----- grid -----
            LazyVerticalGrid(
                columns = GridCells.Fixed(COLS), modifier = Modifier.fillMaxWidth().height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items((1..total).toList()) { n ->
                    val on = n in selected
                    Box(
                        Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (on) MaterialTheme.colorScheme.primary else Color(0x33808080))
                            .clickable { selected = if (on) selected - n else selected + n },
                        contentAlignment = Alignment.Center
                    ) { Text("$n", color = if (on) Color.White else MaterialTheme.colorScheme.onSurface) }
                }
            }

            // ----- schedule -----
            Text("हर दिन कैसे?", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = mode == 0, onClick = { mode = 0 })
                Text("वही पद कल भी दोहराएँ (same every day)")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = mode == 1, onClick = { mode = 1 })
                Text("आगे बढ़ें — series में दोहराव नहीं")
            }
            if (mode == 1) OutlinedTextField(
                perDayStr, { perDayStr = it.filter(Char::isDigit) }, label = { Text("Pads per day") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto random order (शफल)", Modifier.weight(1f))
                Switch(auto, { auto = it })
            }

            Button(
                onClick = {
                    val base = selected.sorted()
                    val series = if (auto) base.shuffled() else base
                    val p = PracticePlan(doc.id, series, mode, (perDayStr.toIntOrNull() ?: 3).coerceAtLeast(1), PracticeStore.today())
                    PracticeStore.save(ctx, p)
                    LibraryRepository.setTotal(doc.id, total)
                    plan = p
                },
                enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth()
            ) { Text("Save plan") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
