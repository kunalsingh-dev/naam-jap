package com.shrijilala.tapcounter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shrijilala.tapcounter.ble.ButtonEventBus
import com.shrijilala.tapcounter.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TapCounterViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val db = CountHistoryDatabase.getInstance(application)
    private val haptics = HapticsHelper(application)

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count

    private val _stealthMode = MutableStateFlow(false)
    val stealthMode: StateFlow<Boolean> = _stealthMode

    /** When true, BLE/headset/volume buttons are ignored — pure manual screen tapping only. */
    private val _hardwareLocked = MutableStateFlow(false)
    val hardwareLocked: StateFlow<Boolean> = _hardwareLocked

    val settings: StateFlow<SettingsRepository.Settings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsRepository.Settings())

    val history: StateFlow<List<CountSession>> = db.countHistoryDao().getAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        _hardwareLocked.value = false
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { s ->
                _hardwareLocked.value = s.hardwareButtonsLocked
            }
        }
        viewModelScope.launch {
            ButtonEventBus.events.collect { event ->
                // Lock means lock: while locked, BLE/headset/volume buttons never
                // touch the count — only manual screen taps do.
                if (_hardwareLocked.value) return@collect
                handleAction(event.action)
            }
        }
    }

    /** Called from the Compose tap gesture. holdDurationMs lets us reject long-presses. */
    fun onScreenTap(holdDurationMs: Long) {
        val threshold = settings.value.longPressThresholdMs
        if (holdDurationMs >= threshold) return // held too long — not counted
        increment()
    }

    fun onVolumeKey(isVolumeUp: Boolean, holdDurationMs: Long) {
        val s = settings.value
        if (!s.volumeButtonsEnabled || _hardwareLocked.value) return
        val threshold = s.longPressThresholdMs
        if (holdDurationMs >= threshold) return
        val action = if (isVolumeUp) s.volumeUpAction else s.volumeDownAction
        handleAction(action)
    }

    private fun handleAction(action: ButtonAction) {
        when (action) {
            ButtonAction.INCREMENT -> increment()
            ButtonAction.DECREMENT -> decrement()
            ButtonAction.STEALTH_TOGGLE -> toggleStealth()
            ButtonAction.STEALTH_FORCE_ON -> forceStealthOn()
            ButtonAction.NONE -> Unit
        }
    }

    private fun increment() {
        _count.value += 1
        haptics.tapTick()
    }

    private fun decrement() {
        if (_count.value > 0) _count.value -= 1
        haptics.tapTick()
    }

    private fun toggleStealth() {
        _stealthMode.value = !_stealthMode.value
        haptics.stealthTogglePattern()
    }

    private fun forceStealthOn() {
        _stealthMode.value = true
        haptics.stealthForceOnPattern()
    }

    fun exitStealthManually() {
        if (_stealthMode.value) {
            _stealthMode.value = false
            haptics.stealthTogglePattern()
        }
    }

    fun setHardwareLocked(locked: Boolean) {
        viewModelScope.launch { settingsRepository.setHardwareButtonsLocked(locked) }
    }

    /** Reset flow: ALWAYS saves the current count + date first, then zeroes the counter. */
    fun confirmReset(label: String? = null) {
        val current = _count.value
        viewModelScope.launch {
            db.countHistoryDao().insert(
                CountSession(finalCount = current, savedAtEpochMillis = System.currentTimeMillis(), label = label)
            )
            _count.value = 0
        }
    }
}
