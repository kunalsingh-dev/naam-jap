package com.shrijilala.tapcounter.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tap_counter_settings")

/**
 * Every "configurable in settings" requirement lives here:
 * - which logical BLE button does what (order can be re-mapped, never to Reset)
 * - which headset media key does what
 * - whether phone volume buttons count taps, and which direction does what
 * - the hold-to-ignore threshold (long-press doesn't count)
 * - the hardware-button lock (so buttons can't interfere with manual tap counting)
 * - the custom BLE device's service/characteristic UUIDs (works with ANY custom board)
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val VOLUME_BUTTONS_ENABLED = booleanPreferencesKey("volume_buttons_enabled")
        val VOLUME_UP_ACTION = stringPreferencesKey("volume_up_action")
        val VOLUME_DOWN_ACTION = stringPreferencesKey("volume_down_action")
        val HARDWARE_BUTTONS_LOCKED = booleanPreferencesKey("hardware_buttons_locked")

        val LONG_PRESS_THRESHOLD_MS = longPreferencesKey("long_press_threshold_ms")

        val BLE_SERVICE_UUID = stringPreferencesKey("ble_service_uuid")
        val BLE_CHARACTERISTIC_UUID = stringPreferencesKey("ble_characteristic_uuid")
        val BLE_BUTTON_A_ACTION = stringPreferencesKey("ble_button_a_action")
        val BLE_BUTTON_B_ACTION = stringPreferencesKey("ble_button_b_action")
        val BLE_BUTTON_MIDDLE_ACTION = stringPreferencesKey("ble_button_middle_action")
        val BLE_LAST_DEVICE_ADDRESS = stringPreferencesKey("ble_last_device_address")

        val MEDIA_INCREMENT_KEY = stringPreferencesKey("media_increment_key")
        val MEDIA_STEALTH_KEY = stringPreferencesKey("media_stealth_key")

        val STEALTH_BRIGHTNESS = floatPreferencesKey("stealth_brightness")
    }

    data class Settings(
        val volumeButtonsEnabled: Boolean = false,
        val volumeUpAction: ButtonAction = ButtonAction.INCREMENT,
        val volumeDownAction: ButtonAction = ButtonAction.NONE,
        val hardwareButtonsLocked: Boolean = false,
        val longPressThresholdMs: Long = 600L,
        val bleServiceUuid: String = "0000FFE0-0000-1000-8000-00805F9B34FB",
        val bleCharacteristicUuid: String = "0000FFE1-0000-1000-8000-00805F9B34FB",
        val bleButtonAAction: ButtonAction = ButtonAction.INCREMENT,
        val bleButtonBAction: ButtonAction = ButtonAction.STEALTH_FORCE_ON,
        val bleButtonMiddleAction: ButtonAction = ButtonAction.NONE,
        val bleLastDeviceAddress: String? = null,
        // KeyEvent key codes as strings, e.g. "HEADSETHOOK", "MEDIA_NEXT"
        val mediaIncrementKey: String = "HEADSETHOOK",
        val mediaStealthKey: String = "MEDIA_NEXT",
        val stealthBrightness: Float = 0.0f
    )

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            volumeButtonsEnabled = prefs[Keys.VOLUME_BUTTONS_ENABLED] ?: false,
            volumeUpAction = ButtonAction.fromName(prefs[Keys.VOLUME_UP_ACTION]).let {
                if (it == ButtonAction.NONE && prefs[Keys.VOLUME_UP_ACTION] == null) ButtonAction.INCREMENT else it
            },
            volumeDownAction = ButtonAction.fromName(prefs[Keys.VOLUME_DOWN_ACTION]),
            hardwareButtonsLocked = prefs[Keys.HARDWARE_BUTTONS_LOCKED] ?: false,
            longPressThresholdMs = prefs[Keys.LONG_PRESS_THRESHOLD_MS] ?: 600L,
            bleServiceUuid = prefs[Keys.BLE_SERVICE_UUID] ?: "0000FFE0-0000-1000-8000-00805F9B34FB",
            bleCharacteristicUuid = prefs[Keys.BLE_CHARACTERISTIC_UUID] ?: "0000FFE1-0000-1000-8000-00805F9B34FB",
            bleButtonAAction = ButtonAction.fromName(prefs[Keys.BLE_BUTTON_A_ACTION]).let {
                if (it == ButtonAction.NONE && prefs[Keys.BLE_BUTTON_A_ACTION] == null) ButtonAction.INCREMENT else it
            },
            bleButtonBAction = ButtonAction.fromName(prefs[Keys.BLE_BUTTON_B_ACTION]).let {
                if (it == ButtonAction.NONE && prefs[Keys.BLE_BUTTON_B_ACTION] == null) ButtonAction.STEALTH_FORCE_ON else it
            },
            bleButtonMiddleAction = ButtonAction.fromName(prefs[Keys.BLE_BUTTON_MIDDLE_ACTION]),
            bleLastDeviceAddress = prefs[Keys.BLE_LAST_DEVICE_ADDRESS],
            mediaIncrementKey = prefs[Keys.MEDIA_INCREMENT_KEY] ?: "HEADSETHOOK",
            mediaStealthKey = prefs[Keys.MEDIA_STEALTH_KEY] ?: "MEDIA_NEXT",
            stealthBrightness = prefs[Keys.STEALTH_BRIGHTNESS] ?: 0.0f
        )
    }

    suspend fun setVolumeButtonsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.VOLUME_BUTTONS_ENABLED] = enabled }

    suspend fun setVolumeUpAction(action: ButtonAction) =
        context.dataStore.edit { it[Keys.VOLUME_UP_ACTION] = action.name }

    suspend fun setVolumeDownAction(action: ButtonAction) =
        context.dataStore.edit { it[Keys.VOLUME_DOWN_ACTION] = action.name }

    suspend fun setHardwareButtonsLocked(locked: Boolean) =
        context.dataStore.edit { it[Keys.HARDWARE_BUTTONS_LOCKED] = locked }

    suspend fun setLongPressThreshold(ms: Long) =
        context.dataStore.edit { it[Keys.LONG_PRESS_THRESHOLD_MS] = ms }

    suspend fun setBleUuids(serviceUuid: String, characteristicUuid: String) =
        context.dataStore.edit {
            it[Keys.BLE_SERVICE_UUID] = serviceUuid
            it[Keys.BLE_CHARACTERISTIC_UUID] = characteristicUuid
        }

    suspend fun setBleButtonMapping(a: ButtonAction, b: ButtonAction, middle: ButtonAction) =
        context.dataStore.edit {
            it[Keys.BLE_BUTTON_A_ACTION] = a.name
            it[Keys.BLE_BUTTON_B_ACTION] = b.name
            it[Keys.BLE_BUTTON_MIDDLE_ACTION] = middle.name
        }

    suspend fun setBleLastDevice(address: String?) =
        context.dataStore.edit {
            if (address == null) it.remove(Keys.BLE_LAST_DEVICE_ADDRESS)
            else it[Keys.BLE_LAST_DEVICE_ADDRESS] = address
        }

    suspend fun setMediaKeys(incrementKey: String, stealthKey: String) =
        context.dataStore.edit {
            it[Keys.MEDIA_INCREMENT_KEY] = incrementKey
            it[Keys.MEDIA_STEALTH_KEY] = stealthKey
        }
}
