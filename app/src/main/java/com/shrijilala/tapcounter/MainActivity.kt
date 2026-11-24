package com.shrijilala.tapcounter

import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.BackHandler
import com.shrijilala.tapcounter.library.LibraryRepository
import com.shrijilala.tapcounter.ui.*
import com.shrijilala.tapcounter.ui.HistoryScreen
import com.shrijilala.tapcounter.ui.ResetDialog
import com.shrijilala.tapcounter.ui.SettingsScreen

private val DarkRed = Color(0xFF8B0000)
private val AccentRed = Color(0xFFB22222)

sealed class Screen {
    object Home : Screen(); object History : Screen(); object Settings : Screen()
    object Library : Screen(); object Practice : Screen(); object Tratak : Screen()
    data class Learn(val docId: String) : Screen()
    data class Reader(val docId: String, val startPad: Int, val filter: List<Int>?, val stealth: Boolean) : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: TapCounterViewModel by viewModels()

    // tracks physical volume-key press start time so we can reject long-holds too
    private var volumeKeyDownAt: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LibraryRepository.init(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val count by viewModel.count.collectAsState()
            val stealth by viewModel.stealthMode.collectAsState()
            val locked by viewModel.hardwareLocked.collectAsState()
            val settings by viewModel.settings.collectAsState()
            var screen by remember { mutableStateOf<Screen>(Screen.Home) }
            var showResetDialog by remember { mutableStateOf(false) }

            ApplyStealthWindowEffects(window, stealth, settings.stealthBrightness)
            BackHandler(enabled = screen !is Screen.Home) {
                screen = when (screen) {
                    is Screen.Reader, is Screen.Practice, is Screen.Learn -> Screen.Library
                    else -> Screen.Home
                }
            }

            MaterialTheme(colorScheme = darkColorScheme(primary = DarkRed, secondary = AccentRed)) {
                when (screen) {
                    Screen.Home -> HomeScreen(
                        count = count,
                        stealth = stealth,
                        locked = locked,
                        onTap = { holdMs -> viewModel.onScreenTap(holdMs) },
                        onExitStealth = { viewModel.exitStealthManually() },
                        onOpenHistory = { screen = Screen.History },
                        onOpenSettings = { screen = Screen.Settings },
                        onOpenLibrary = { screen = Screen.Library },
                        onOpenTratak = { screen = Screen.Tratak },
                        onRequestReset = { showResetDialog = true },
                        onToggleLock = { viewModel.setHardwareLocked(!locked) }
                    )
                    Screen.History -> HistoryScreen(
                        sessions = viewModel.history.collectAsState().value,
                        onBack = { screen = Screen.Home }
                    )
                    Screen.Library -> LibraryScreen(
                        onBack = { screen = Screen.Home },
                        onOpen = { id -> screen = Screen.Reader(id, 1, null, false) },
                        onLearn = { id -> screen = Screen.Learn(id) },
                        onPractice = { screen = Screen.Practice }
                    )
                    is Screen.Learn -> LearnPadScreen(
                        docId = (screen as Screen.Learn).docId,
                        onBack = { screen = Screen.Library },
                        onOpen = { id, no -> screen = Screen.Reader(id, no, null, false) }
                    )
                    Screen.Practice -> PracticeScreen(
                        onBack = { screen = Screen.Library },
                        onStart = { id, pads, st -> screen = Screen.Reader(id, pads.first(), pads, st) }
                    )
                    is Screen.Reader -> {
                        val r = screen as Screen.Reader
                        ReaderScreen(r.docId, r.startPad, r.filter, r.stealth, onBack = {
                            screen = if (r.filter != null) Screen.Practice else Screen.Library
                        })
                    }
                    Screen.Tratak -> TratakScreen(onBack = { screen = Screen.Home })
                    Screen.Settings -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = { screen = Screen.Home }
                    )
                }

                if (showResetDialog) {
                    ResetDialog(
                        currentCount = count,
                        onConfirm = {
                            viewModel.confirmReset()
                            showResetDialog = false
                        },
                        onDismiss = { showResetDialog = false }
                    )
                }
            }
        }
    }

    /** Volume buttons: consumed (not sent to system volume) only while the feature is enabled. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (viewModel.settings.value.volumeButtonsEnabled && !viewModel.hardwareLocked.value) {
                if (event?.repeatCount == 0) volumeKeyDownAt = SystemClock.elapsedRealtime()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (viewModel.settings.value.volumeButtonsEnabled && !viewModel.hardwareLocked.value) {
                val duration = SystemClock.elapsedRealtime() - volumeKeyDownAt
                viewModel.onVolumeKey(isVolumeUp = keyCode == KeyEvent.KEYCODE_VOLUME_UP, holdDurationMs = duration)
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }
}

/** Blacks out the screen, hides system bars (incl. the clock), and dims brightness in stealth mode. */
@Composable
private fun ApplyStealthWindowEffects(window: Window, stealth: Boolean, stealthBrightness: Float) {
    val view = LocalView.current
    LaunchedEffect(stealth) {
        val controller = WindowInsetsControllerCompat(window, view)
        if (stealth) {
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            val params = window.attributes
            params.screenBrightness = stealthBrightness.coerceIn(0.01f, 1f)
            window.attributes = params
        } else {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val params = window.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = params
        }
    }
}

@Composable
private fun HomeScreen(
    count: Int,
    stealth: Boolean,
    locked: Boolean,
    onTap: (Long) -> Unit,
    onExitStealth: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenTratak: () -> Unit,
    onRequestReset: () -> Unit,
    onToggleLock: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (stealth) Color.Black else Color(0xFF120202))
            .pointerInput(stealth) {
                detectTapGestures(
                    onPress = {
                        val start = SystemClock.elapsedRealtime()
                        val released = tryAwaitRelease()
                        val duration = SystemClock.elapsedRealtime() - start
                        if (released) onTap(duration)
                    },
                    onLongPress = { if (stealth) onExitStealth() }
                )
            }
    ) {
        // In stealth mode, absolutely nothing is drawn — count, buttons, everything hidden.
        AnimatedVisibility(
            visible = !stealth,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                Text(
                    text = count.toString(),
                    color = Color.White,
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 40.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleLock) {
                        Icon(
                            imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = if (locked) "Hardware buttons locked" else "Hardware buttons unlocked",
                            tint = if (locked) AccentRed else Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Color.White)
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("History") }, onClick = {
                                menuExpanded = false; onOpenHistory()
                            })
                            DropdownMenuItem(text = { Text("पाठ संग्रह (Library)") }, onClick = {
                                menuExpanded = false; onOpenLibrary()
                            })
                            DropdownMenuItem(text = { Text("Tratak HUD") }, onClick = {
                                menuExpanded = false; onOpenTratak()
                            })
                            DropdownMenuItem(text = { Text("Settings") }, onClick = {
                                menuExpanded = false; onOpenSettings()
                            })
                            DropdownMenuItem(text = { Text("Reset counter") }, onClick = {
                                menuExpanded = false; onRequestReset()
                            })
                        }
                    }
                }
            }
        }
    }
}
