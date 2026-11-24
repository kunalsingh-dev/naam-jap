package com.shrijilala.tapcounter.ble

import com.shrijilala.tapcounter.data.ButtonAction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

enum class InputSource { BLE_RING, HEADSET, VOLUME_KEY }

data class ButtonEvent(val action: ButtonAction, val source: InputSource)

/**
 * Simple process-wide bus. The BLE foreground service, the media-button
 * receiver, and the volume-key handler in MainActivity all post here;
 * TapCounterViewModel is the single collector that decides what happens.
 */
object ButtonEventBus {
    private val _events = MutableSharedFlow<ButtonEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ButtonEvent> = _events

    suspend fun emit(event: ButtonEvent) {
        _events.emit(event)
    }

    fun tryEmit(event: ButtonEvent) {
        _events.tryEmit(event)
    }
}
