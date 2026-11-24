package com.shrijilala.tapcounter.data

/**
 * What a physical button (BLE ring button, headset button, or phone volume key)
 * is allowed to do. RESET is intentionally NOT an option here — reset is only ever
 * reachable through the three-dot menu + confirmation dialog, so a stray button
 * press (in a pocket, mid-call, etc.) can never wipe a count.
 */
enum class ButtonAction {
    INCREMENT,
    DECREMENT,
    STEALTH_TOGGLE,
    STEALTH_FORCE_ON,   // the "negative" button behaviour you described
    NONE;

    companion object {
        fun fromName(name: String?): ButtonAction =
            entries.firstOrNull { it.name == name } ?: NONE
    }
}
