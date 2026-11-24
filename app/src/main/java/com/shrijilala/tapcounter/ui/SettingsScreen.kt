package com.shrijilala.tapcounter.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shrijilala.tapcounter.TapCounterViewModel
import com.shrijilala.tapcounter.ble.BleForegroundService
import com.shrijilala.tapcounter.ble.BleTapManager
import com.shrijilala.tapcounter.data.ButtonAction
import com.shrijilala.tapcounter.data.SettingsRepository
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TapCounterViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { SettingsRepository(context) }
    val settings by viewModel.settings.collectAsState()
    val hardwareLocked by viewModel.hardwareLocked.collectAsState()

    val bleManager = remember { BleTapManager(context.applicationContext, settingsRepository) }
    val foundDevices by bleManager.foundDevices.collectAsState()
    val connectionState by bleManager.connectionState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.all { it }) bleManager.startScan()
    }

    DisposableEffect(Unit) { onDispose { bleManager.stopScan() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            item { SectionHeader("Manual tap") }
            item {
                LabeledSlider(
                    label = "Ignore holds longer than ${settings.longPressThresholdMs} ms",
                    value = settings.longPressThresholdMs.toFloat(),
                    range = 200f..1500f,
                    onChange = { scope.launch { settingsRepository.setLongPressThreshold(it.toLong()) } }
                )
            }

            item { SectionHeader("Hardware button lock") }
            item {
                SwitchRow(
                    label = "Lock hardware buttons",
                    subtitle = "While on, BLE ring / headset / volume buttons are ignored — only screen taps count.",
                    checked = hardwareLocked,
                    onCheckedChange = { viewModel.setHardwareLocked(it) }
                )
            }

            item { SectionHeader("Phone volume buttons") }
            item {
                SwitchRow(
                    label = "Use volume buttons to count",
                    subtitle = null,
                    checked = settings.volumeButtonsEnabled,
                    onCheckedChange = { scope.launch { settingsRepository.setVolumeButtonsEnabled(it) } }
                )
            }
            if (settings.volumeButtonsEnabled) {
                item {
                    ActionPicker(
                        label = "Volume UP does",
                        current = settings.volumeUpAction,
                        onSelect = { scope.launch { settingsRepository.setVolumeUpAction(it) } }
                    )
                }
                item {
                    ActionPicker(
                        label = "Volume DOWN does",
                        current = settings.volumeDownAction,
                        onSelect = { scope.launch { settingsRepository.setVolumeDownAction(it) } }
                    )
                }
            }

            item { SectionHeader("Bluetooth headset") }
            item {
                Text(
                    "Works with any standard Bluetooth headset. Choose which physical button (the ones your headset already has) triggers each action.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                MediaKeyPicker(
                    label = "Increment button",
                    current = settings.mediaIncrementKey,
                    onSelect = { key -> scope.launch { settingsRepository.setMediaKeys(key, settings.mediaStealthKey) } }
                )
            }
            item {
                MediaKeyPicker(
                    label = "Stealth-toggle button",
                    current = settings.mediaStealthKey,
                    onSelect = { key -> scope.launch { settingsRepository.setMediaKeys(settings.mediaIncrementKey, key) } }
                )
            }

            item { SectionHeader("Custom BLE ring") }
            item {
                OutlinedTextField(
                    value = settings.bleServiceUuid,
                    onValueChange = { scope.launch { settingsRepository.setBleUuids(it, settings.bleCharacteristicUuid) } },
                    label = { Text("Service UUID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.bleCharacteristicUuid,
                    onValueChange = { scope.launch { settingsRepository.setBleUuids(settings.bleServiceUuid, it) } },
                    label = { Text("Characteristic UUID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                ActionPicker(
                    label = "Button A does (order can be changed here)",
                    current = settings.bleButtonAAction,
                    onSelect = {
                        scope.launch { settingsRepository.setBleButtonMapping(it, settings.bleButtonBAction, settings.bleButtonMiddleAction) }
                    }
                )
            }
            item {
                ActionPicker(
                    label = "Button B does (your \"negative\"/stealth button)",
                    current = settings.bleButtonBAction,
                    onSelect = {
                        scope.launch { settingsRepository.setBleButtonMapping(settings.bleButtonAAction, it, settings.bleButtonMiddleAction) }
                    }
                )
            }
            item {
                ActionPicker(
                    label = "Middle button does (can't be set to Reset — reset only via ⋮ menu)",
                    current = settings.bleButtonMiddleAction,
                    onSelect = {
                        scope.launch { settingsRepository.setBleButtonMapping(settings.bleButtonAAction, settings.bleButtonBAction, it) }
                    }
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                Button(onClick = { requestBlePermissionsAndScan(context, permissionLauncher, bleManager) }) {
                    Text(
                        when (connectionState) {
                            BleTapManager.ConnectionState.SCANNING -> "Scanning…"
                            BleTapManager.ConnectionState.CONNECTED -> "Connected — rescan"
                            else -> "Scan for ring"
                        }
                    )
                }
            }

            items(foundDevices) { device: BluetoothDevice ->
                DeviceRow(device = device) {
                    val intent = Intent(context, BleForegroundService::class.java)
                        .putExtra(BleForegroundService.EXTRA_DEVICE_ADDRESS, device.address)
                    ContextCompat.startForegroundService(context, intent)
                    bleManager.stopScan()
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun requestBlePermissionsAndScan(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    bleManager: BleTapManager
) {
    val needed = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        needed += Manifest.permission.BLUETOOTH_SCAN
        needed += Manifest.permission.BLUETOOTH_CONNECT
    } else {
        needed += Manifest.permission.ACCESS_FINE_LOCATION
    }
    val allGranted = needed.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    if (allGranted) bleManager.startScan() else launcher.launch(needed.toTypedArray())
}

@Composable
private fun SectionHeader(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(text, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun SwitchRow(label: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionPicker(label: String, current: ButtonAction, onSelect: (ButtonAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = current.name,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ButtonAction.entries.forEach { action ->
                DropdownMenuItem(text = { Text(action.name) }, onClick = { onSelect(action); expanded = false })
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaKeyPicker(label: String, current: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("HEADSETHOOK", "PLAY_PAUSE", "MEDIA_NEXT", "MEDIA_PREVIOUS")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = current,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { key ->
                DropdownMenuItem(text = { Text(key) }, onClick = { onSelect(key); expanded = false })
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun DeviceRow(device: BluetoothDevice, onClick: () -> Unit) {
    val name = try { device.name ?: device.address } catch (e: SecurityException) { device.address }
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text(device.address) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
