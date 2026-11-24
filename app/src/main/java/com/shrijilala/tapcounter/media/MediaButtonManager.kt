package com.shrijilala.tapcounter.media

import android.content.Context
import android.view.KeyEvent
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.shrijilala.tapcounter.ble.ButtonEvent
import com.shrijilala.tapcounter.ble.ButtonEventBus
import com.shrijilala.tapcounter.ble.InputSource
import com.shrijilala.tapcounter.data.ButtonAction
import com.shrijilala.tapcounter.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Works with "any bluetooth headset" — standard Android media-button events,
 * no pairing-specific code needed. Which key does what is configurable in Settings.
 */
class MediaButtonManager(
    context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val mediaSession = MediaSessionCompat(context, "TapCounterMediaSession").apply {
        setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .build()
        )
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: android.content.Intent): Boolean {
                val event = mediaButtonIntent.getParcelableExtra<KeyEvent>(android.content.Intent.EXTRA_KEY_EVENT)
                if (event != null && event.action == KeyEvent.ACTION_DOWN) {
                    onKey(keyName(event.keyCode))
                    return true
                }
                return super.onMediaButtonEvent(mediaButtonIntent)
            }
        })
        isActive = true
    }

    private fun keyName(keyCode: Int): String = when (keyCode) {
        KeyEvent.KEYCODE_HEADSETHOOK -> "HEADSETHOOK"
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "PLAY_PAUSE"
        KeyEvent.KEYCODE_MEDIA_NEXT -> "MEDIA_NEXT"
        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "MEDIA_PREVIOUS"
        else -> "UNKNOWN"
    }

    private fun onKey(name: String) {
        scope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val action = when (name) {
                settings.mediaIncrementKey -> ButtonAction.INCREMENT
                settings.mediaStealthKey -> ButtonAction.STEALTH_TOGGLE
                else -> ButtonAction.NONE
            }
            if (action != ButtonAction.NONE) {
                ButtonEventBus.emit(ButtonEvent(action, InputSource.HEADSET))
            }
        }
    }

    fun release() {
        mediaSession.isActive = false
        mediaSession.release()
    }
}
