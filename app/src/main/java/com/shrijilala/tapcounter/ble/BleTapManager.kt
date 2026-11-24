package com.shrijilala.tapcounter.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.shrijilala.tapcounter.data.ButtonAction
import com.shrijilala.tapcounter.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Talks to ANY BLE peripheral (your nRF52840 ring, or a different custom board later)
 * without needing its exact identity baked in — the service/characteristic UUIDs come
 * from Settings, and can be changed there without touching this code.
 *
 * Expected wire protocol: the peripheral sends a single-byte GATT notification
 * on the configured characteristic:
 *   0x01 = "button A" pressed
 *   0x02 = "button B" pressed  (your "negative"/stealth button)
 *   0x03 = "middle button" pressed
 * Which logical button maps to which ButtonAction is configurable in Settings,
 * and the middle button can never be assigned to Reset.
 */
@SuppressLint("MissingPermission")
class BleTapManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter get() = bluetoothManager.adapter

    private var gatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _foundDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDevice>> = _foundDevices

    enum class ConnectionState { DISCONNECTED, SCANNING, CONNECTING, CONNECTED }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            if (_foundDevices.value.none { it.address == device.address }) {
                _foundDevices.value = _foundDevices.value + device
            }
        }
    }

    fun startScan() {
        if (adapter == null || !adapter.isEnabled) return
        _foundDevices.value = emptyList()
        _connectionState.value = ConnectionState.SCANNING
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        adapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)
    }

    fun stopScan() {
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun connect(device: BluetoothDevice) {
        stopScan()
        _connectionState.value = ConnectionState.CONNECTING
        gatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.CONNECTED
                    scope.launch { settingsRepository.setBleLastDevice(g.device.address) }
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            scope.launch {
                val settings = settingsRepository.settingsFlow.first()
                val serviceUuid = runCatching { UUID.fromString(settings.bleServiceUuid) }.getOrNull() ?: return@launch
                val charUuid = runCatching { UUID.fromString(settings.bleCharacteristicUuid) }.getOrNull() ?: return@launch

                val characteristic = g.getService(serviceUuid)?.getCharacteristic(charUuid) ?: return@launch
                g.setCharacteristicNotification(characteristic, true)
                val cccd = characteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                )
                if (cccd != null) {
                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    g.writeDescriptor(cccd)
                }
            }
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value ?: return
            if (value.isEmpty()) return
            handleRawButtonCode(value[0].toInt())
        }
    }

    private fun handleRawButtonCode(code: Int) {
        scope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val action = when (code) {
                1 -> settings.bleButtonAAction
                2 -> settings.bleButtonBAction
                3 -> settings.bleButtonMiddleAction
                else -> ButtonAction.NONE
            }
            if (action != ButtonAction.NONE) {
                ButtonEventBus.emit(ButtonEvent(action, InputSource.BLE_RING))
            }
        }
    }
}
