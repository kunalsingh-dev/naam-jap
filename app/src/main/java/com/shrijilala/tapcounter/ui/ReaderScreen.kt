package com.shrijilala.tapcounter.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shrijilala.tapcounter.HapticsHelper
import com.shrijilala.tapcounter.library.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    docId: String,
    startPad: Int,
    filter: List<Int>?,
    stealth: Boolean,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val window = (ctx as ComponentActivity).window
    val prefs = remember { ReaderPrefs(ctx) }
    val doc = LibraryRepository.docs.firstOrNull { it.id == docId }
    if (doc == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Text not found") }
        return
    }

    val pads: List<Pad> = if (filter != null)
        filter.map { n -> doc.pads.firstOrNull { it.no == n } ?: Pad(n) }
    else doc.pads.sortedBy { it.no }

    // ---------- STEALTH PRACTICE: black screen, tap = haptic + next pad, long-press = exit ----------
    if (stealth) {
        val haptics = remember { HapticsHelper(ctx) }
        var idx by remember { mutableIntStateOf(0) }
        StealthWindow(window, on = true)
        Box(
            Modifier.fillMaxSize().background(Color.Black).pointerInput(pads.size) {
                detectTapGestures(
                    onTap = {
                        if (idx < pads.size - 1) { idx++; haptics.tapTick() }
                        else { haptics.stealthForceOnPattern(); onBack() }
                    },
                    onLongPress = { haptics.stealthTogglePattern(); onBack() }
                )
            }
        )
        return
    }

    val (bg, fg, accent) = prefs.colors()
    val scope = rememberCoroutineScope()
    val startIdx = pads.indexOfFirst { it.no == startPad }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = startIdx, pageCount = { pads.size })
    val lazy = rememberLazyListState(initialFirstVisibleItemIndex = startIdx)
    val currentIdx = if (prefs.scrollMode == 0) pager.currentPage else lazy.firstVisibleItemIndex

    fun goTo(i: Int) {
        if (pads.isEmpty()) return
        val t = i.coerceIn(0, pads.size - 1)
        scope.launch { if (prefs.scrollMode == 0) pager.animateScrollToPage(t) else lazy.animateScrollToItem(t) }
    }

    var showSettings by remember { mutableStateOf(false) }
    var showIndex by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var langMenu by remember { mutableStateOf(false) }

    // audio
    val holder = remember { arrayOfNulls<MediaPlayer>(1) }
    var playing by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { runCatching { holder[0]?.release() } } }
    fun toggleAudio() {
        if (playing) {
            runCatching { holder[0]?.release() }; holder[0] = null; playing = false; return
        }
        if (doc.audioUrl.isBlank()) {
            Toast.makeText(ctx, "Audio link not set — tap ✎ and add an audio URL", Toast.LENGTH_LONG).show(); return
        }
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build()
            )
            mp.setDataSource(doc.audioUrl)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { playing = false }
            mp.setOnErrorListener { _, _, _ -> playing = false; true }
            mp.prepareAsync()
            holder[0] = mp; playing = true
        } catch (e: Exception) {
            Toast.makeText(ctx, "Can't play audio: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Column(Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
        // top bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = fg) }
            Text(
                doc.title, color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, modifier = Modifier.weight(1f)
            )
            Box {
                TextButton(onClick = { langMenu = true }) {
                    Text(LANGUAGES[prefs.lang] ?: prefs.lang, color = accent)
                }
                DropdownMenu(expanded = langMenu, onDismissRequest = { langMenu = false }) {
                    LANGUAGES.forEach { (code, name) ->
                        DropdownMenuItem(text = { Text(name) }, onClick = {
                            prefs.lang = code; prefs.save(); langMenu = false
                        })
                    }
                }
            }
            IconButton(onClick = { showEdit = true }) { Icon(Icons.Filled.Edit, "Edit", tint = fg) }
            IconButton(onClick = { showSettings = true }) { Icon(Icons.Filled.Settings, "Look", tint = fg) }
        }

        // body
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (pads.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("अभी कोई पाठ नहीं है", color = fg, fontSize = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("✎ दबाकर पाठ जोड़ें — एक साथ कई पद भी paste कर सकते हैं।", color = fg.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                }
            } else if (prefs.scrollMode == 0) {
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                    PadContent(pads[page], doc, prefs, scrollable = true, modifier = Modifier.fillMaxSize())
                }
            } else {
                LazyColumn(state = lazy, modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(pads) { _, pad ->
                        PadContent(pad, doc, prefs, scrollable = false, modifier = Modifier.fillMaxWidth())
                        HorizontalDivider(color = fg.copy(alpha = 0.15f))
                    }
                }
            }
        }

        // bottom bar
        Row(
            Modifier.fillMaxWidth().background(Color(0x33808080)).navigationBarsPadding().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showIndex = true }) { Icon(Icons.Filled.FormatListNumbered, "Index", tint = fg) }
            IconButton(onClick = { prefs.fontSize = (prefs.fontSize - 2f).coerceAtLeast(12f); prefs.save() }) { Icon(Icons.Filled.Remove, "Smaller", tint = fg) }
            IconButton(onClick = { prefs.fontSize = (prefs.fontSize + 2f).coerceAtMost(48f); prefs.save() }) { Icon(Icons.Filled.Add, "Bigger", tint = fg) }
            IconButton(onClick = { prefs.reset() }) { Icon(Icons.Filled.Refresh, "Reset look", tint = fg) }
            IconButton(onClick = { toggleAudio() }) {
                Icon(if (playing) Icons.Filled.Stop else Icons.Filled.MusicNote, "Audio", tint = if (playing) accent else fg)
            }
            IconButton(onClick = { goTo(currentIdx - 1) }) { Icon(Icons.Filled.ChevronLeft, "Prev", tint = fg) }
            IconButton(onClick = { goTo(currentIdx + 1) }) { Icon(Icons.Filled.ChevronRight, "Next", tint = fg) }
        }
    }

    if (showIndex) {
        AlertDialog(
            onDismissRequest = { showIndex = false },
            title = { Text("Index") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    itemsIndexed(pads) { i, p ->
                        val first = (p.text[prefs.lang] ?: p.text["hi"] ?: "").lineSequence().firstOrNull() ?: ""
                        Text(
                            "${p.no}.  $first", maxLines = 1,
                            modifier = Modifier.fillMaxWidth().clickable { showIndex = false; goTo(i) }.padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showIndex = false }) { Text("Close") } }
        )
    }

    if (showSettings) LookDialog(prefs, doc.hasMeaning || doc.pads.any { it.meaning.isNotEmpty() }) { showSettings = false }

    if (showEdit) {
        val cur = pads.getOrNull(currentIdx)
        EditPadDialog(
            doc = doc, lang = prefs.lang,
            startNo = cur?.no ?: ((doc.pads.maxOfOrNull { it.no } ?: 0) + 1),
            onDismiss = { showEdit = false }
        )
    }
}

@Composable
private fun PadContent(pad: Pad, doc: TextDoc, prefs: ReaderPrefs, scrollable: Boolean, modifier: Modifier) {
    val (_, fg, accent) = prefs.colors()
    val lang = prefs.lang
    val txt = pad.text[lang] ?: pad.text["hi"] ?: ""
    val meaning = pad.meaning[lang] ?: pad.meaning["hi"] ?: ""
    val base = if (scrollable) modifier.verticalScroll(rememberScrollState()) else modifier
    Column(base.padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${pad.no}.", color = accent, fontSize = (prefs.fontSize + 8f).sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (txt.isBlank()) {
            Text("— पाठ खाली है (✎ से जोड़ें) —", color = fg.copy(alpha = 0.6f))
        } else {
            txt.split("\n").forEach { line ->
                Text(
                    line, color = fg, fontSize = prefs.fontSize.sp,
                    lineHeight = (prefs.fontSize * prefs.lineSpacing).sp,
                    textAlign = prefs.align(), fontFamily = prefs.family(),
                    fontWeight = if (prefs.bold) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (prefs.showMeaning && meaning.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = accent.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Text(
                meaning, color = fg.copy(alpha = 0.85f), fontSize = (prefs.fontSize - 2f).sp,
                lineHeight = ((prefs.fontSize - 2f) * prefs.lineSpacing).sp,
                textAlign = prefs.align(), fontFamily = prefs.family(), modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Swatches(colors: List<Long>, onPick: (Long) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { c ->
            Box(Modifier.size(34.dp).clip(CircleShape).background(Color(c)).clickable { onPick(c) })
        }
    }
}

@Composable
private fun LookDialog(prefs: ReaderPrefs, hasMeaning: Boolean, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Look & style") },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Theme")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReaderPrefs.THEMES.forEachIndexed { i, (name, _) ->
                        FilterChip(selected = prefs.themeIdx == i, onClick = { prefs.themeIdx = i; prefs.save() }, label = { Text(name) })
                    }
                }
                Text("Custom background")
                Swatches(ReaderPrefs.PALETTE) { prefs.customBg = it.toInt(); prefs.themeIdx = -1; prefs.save() }
                Text("Custom text color")
                Swatches(ReaderPrefs.PALETTE) { prefs.customFg = it.toInt(); prefs.themeIdx = -1; prefs.save() }

                Text("Text size: ${prefs.fontSize.toInt()}")
                Slider(prefs.fontSize, { prefs.fontSize = it; prefs.save() }, valueRange = 12f..48f)
                Text("Line spacing: ${"%.1f".format(prefs.lineSpacing)}")
                Slider(prefs.lineSpacing, { prefs.lineSpacing = it; prefs.save() }, valueRange = 1.0f..2.4f)

                Text("Font")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReaderPrefs.FONT_NAMES.forEachIndexed { i, n ->
                        FilterChip(selected = prefs.fontIdx == i, onClick = { prefs.fontIdx = i; prefs.save() }, label = { Text(n) })
                    }
                }
                Text("Alignment")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Left", "Center", "Right").forEachIndexed { i, n ->
                        FilterChip(selected = prefs.alignIdx == i, onClick = { prefs.alignIdx = i; prefs.save() }, label = { Text(n) })
                    }
                }
                Text("Scroll style")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = prefs.scrollMode == 0, onClick = { prefs.scrollMode = 0; prefs.save() }, label = { Text("Swipe pages") })
                    FilterChip(selected = prefs.scrollMode == 1, onClick = { prefs.scrollMode = 1; prefs.save() }, label = { Text("Scroll down") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bold", Modifier.weight(1f))
                    Switch(prefs.bold, { prefs.bold = it; prefs.save() })
                }
                if (hasMeaning) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show meaning (अर्थ)", Modifier.weight(1f))
                    Switch(prefs.showMeaning, { prefs.showMeaning = it; prefs.save() })
                }
            }
        }
    )
}

@Composable
private fun EditPadDialog(doc: TextDoc, lang: String, startNo: Int, onDismiss: () -> Unit) {
    var noStr by remember { mutableStateOf(startNo.toString()) }
    val no = noStr.toIntOrNull() ?: 0
    val existing = doc.pads.firstOrNull { it.no == no }
    var text by remember(no, lang) { mutableStateOf(existing?.text?.get(lang) ?: "") }
    var meaning by remember(no, lang) { mutableStateOf(existing?.meaning?.get(lang) ?: "") }
    var audio by remember { mutableStateOf(doc.audioUrl) }
    var bulk by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit — ${LANGUAGES[lang] ?: lang}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(noStr, { noStr = it.filter(Char::isDigit) }, label = { Text("Pad number") }, singleLine = true)
                OutlinedTextField(text, { text = it }, label = { Text("Text (one line per row)") }, minLines = 5, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(meaning, { meaning = it }, label = { Text("Meaning / अर्थ (optional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bulk paste: blank line = next pad", Modifier.weight(1f))
                    Switch(bulk, { bulk = it })
                }
                OutlinedTextField(audio, { audio = it }, label = { Text("Audio URL (https…)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (no > 0) {
                    if (bulk) {
                        val blocks = text.split(Regex("\n\\s*\n")).map { it.trim() }.filter { it.isNotEmpty() }
                        val mBlocks = meaning.split(Regex("\n\\s*\n")).map { it.trim() }.filter { it.isNotEmpty() }
                        blocks.forEachIndexed { i, b ->
                            LibraryRepository.upsert(doc.id, no + i, lang, b, mBlocks.getOrNull(i))
                        }
                    } else {
                        LibraryRepository.upsert(doc.id, no, lang, text, meaning)
                    }
                }
                if (audio != doc.audioUrl) LibraryRepository.setAudio(doc.id, audio.trim())
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
