package com.shrijilala.tapcounter.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.shrijilala.tapcounter.HapticsHelper
import kotlinx.coroutines.delay

private val NAME_COLORS = listOf(0xFFFFFFFF, 0xFFFFB300, 0xFFFF5252, 0xFFFFD54F, 0xFF69F0AE, 0xFF40C4FF)

/**
 * Live HUD / Tratak: live camera with a custom name drawn over it, so the name
 * is seen everywhere you look. Nothing is recorded or saved — preview only.
 */
@Composable
fun TratakScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val window = (ctx as ComponentActivity).window
    val sp = remember { ctx.getSharedPreferences("tratak", Context.MODE_PRIVATE) }
    val haptics = remember { HapticsHelper(ctx) }

    var name by remember { mutableStateOf(sp.getString("name", "श्री राधा") ?: "श्री राधा") }
    var size by remember { mutableFloatStateOf(sp.getFloat("size", 56f)) }
    var colorIdx by remember { mutableIntStateOf(sp.getInt("color", 1)) }
    var opacity by remember { mutableFloatStateOf(sp.getFloat("opacity", 0.9f)) }
    var glow by remember { mutableStateOf(sp.getBoolean("glow", true)) }
    var repeatGrid by remember { mutableStateOf(sp.getBoolean("grid", false)) }
    var pulse by remember { mutableStateOf(sp.getBoolean("pulse", true)) }
    var front by remember { mutableStateOf(sp.getBoolean("front", false)) }
    var chimeMin by remember { mutableIntStateOf(sp.getInt("chime", 0)) }

    var panel by remember { mutableStateOf(true) }
    var stealth by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var secs by remember { mutableIntStateOf(0) }

    fun persist() {
        sp.edit().putString("name", name).putFloat("size", size).putInt("color", colorIdx)
            .putFloat("opacity", opacity).putBoolean("glow", glow).putBoolean("grid", repeatGrid)
            .putBoolean("pulse", pulse).putBoolean("front", front).putInt("chime", chimeMin).apply()
    }

    var hasCam by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCam = it }
    LaunchedEffect(Unit) { if (!hasCam) launcher.launch(Manifest.permission.CAMERA) }

    LaunchedEffect(running) {
        while (running) {
            delay(1000); secs++
            if (chimeMin > 0 && secs % (chimeMin * 60) == 0) haptics.stealthTogglePattern()
        }
    }

    StealthWindow(window, on = stealth, brightness = 0.05f)

    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulseA by infinite.animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a"
    )
    val alpha = (if (pulse) pulseA else 1f) * opacity
    val c = Color(NAME_COLORS[colorIdx.coerceIn(0, NAME_COLORS.size - 1)])
    val style = TextStyle(
        color = c.copy(alpha = alpha), fontSize = size.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        shadow = if (glow) Shadow(color = c, offset = Offset.Zero, blurRadius = 32f) else null
    )

    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(stealth) {
            if (stealth) detectTapGestures(onLongPress = { stealth = false; haptics.stealthTogglePattern() })
        }
    ) {
        if (hasCam) CameraLayer(front) else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission needed", color = Color.White)
        }

        // the name HUD
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (repeatGrid) {
                Column(Modifier.fillMaxSize().padding(vertical = 24.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                    repeat(6) { Text("$name  $name", style = style.copy(fontSize = (size * 0.6f).sp), maxLines = 1, modifier = Modifier.fillMaxWidth()) }
                }
            } else {
                Text(name, style = style)
            }
        }

        if (!stealth) {
            Row(
                Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
                Text("%02d:%02d".format(secs / 60, secs % 60), color = Color.White)
            }
            IconButton(
                onClick = { panel = !panel },
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding()
            ) { Icon(Icons.Filled.Tune, "Controls", tint = Color.White) }

            if (panel) {
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                        .background(Color(0xCC000000)).padding(12.dp).heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        name, { name = it; persist() }, label = { Text("Name / mantra to show") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Text("Size", color = Color.White)
                    Slider(size, { size = it; persist() }, valueRange = 24f..140f)
                    Text("Opacity", color = Color.White)
                    Slider(opacity, { opacity = it; persist() }, valueRange = 0.2f..1f)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NAME_COLORS.forEachIndexed { i, col ->
                            Box(Modifier.size(34.dp).clip(CircleShape).background(Color(col)).clickable { colorIdx = i; persist() })
                        }
                    }
                    ToggleRow("Glow", glow) { glow = it; persist() }
                    ToggleRow("Repeat across screen", repeatGrid) { repeatGrid = it; persist() }
                    ToggleRow("Slow pulse", pulse) { pulse = it; persist() }
                    ToggleRow("Front camera", front) { front = it; persist() }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vibrate every ${if (chimeMin == 0) "— (off)" else "$chimeMin min"}", color = Color.White, modifier = Modifier.weight(1f))
                        TextButton(onClick = { chimeMin = (chimeMin - 1).coerceAtLeast(0); persist() }) { Text("−") }
                        TextButton(onClick = { chimeMin = (chimeMin + 1).coerceAtMost(60); persist() }) { Text("+") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { running = !running }) { Text(if (running) "Pause timer" else "Start timer") }
                        OutlinedButton(onClick = { secs = 0; running = false }) { Text("Reset") }
                        Button(onClick = { stealth = true; panel = false; haptics.stealthTogglePattern() }) { Text("Stealth") }
                    }
                    Text("Stealth: controls hidden, screen dimmed, touch locked. Long-press to exit.", color = Color(0xAAFFFFFF), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Switch(value, onChange)
    }
}

@Composable
private fun CameraLayer(front: Boolean) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    DisposableEffect(front, owner) {
        val future = ProcessCameraProvider.getInstance(ctx)
        future.addListener({
            runCatching {
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val sel = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                provider.unbindAll()
                provider.bindToLifecycle(owner, sel, preview)
            }
        }, ContextCompat.getMainExecutor(ctx))
        onDispose { runCatching { future.get().unbindAll() } }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}
