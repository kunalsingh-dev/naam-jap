# Tap Counter

Native Android app (Kotlin + Jetpack Compose). Covers:

1. **Manual tap counting** — whole screen is tappable, count shown centered, registers on release so a quick tap is instant.
2. **Stealth mode** — screen goes fully black, system status bar (with the clock) is hidden, brightness drops, no text/UI is drawn at all. Taps still count and give a quiet vibration each time. Exit with a long-press anywhere, or the BLE ring's stealth button.
3. **Bluetooth counting** — works two ways:
   - **Any standard headset**: uses Android's normal media-button API, no pairing-specific code needed.
   - **Your custom ring** (or any custom BLE board): connects over BLE GATT. Service/characteristic UUIDs and the button→action mapping are all editable in Settings — nothing is hardcoded to one device.
4. **History & accident-proof reset**: reset is only reachable via the ⋮ menu, with a confirmation dialog that shows the count that's about to be saved. It's written to the History database *before* the counter is zeroed. No hardware button (BLE middle button included) can be mapped to Reset — it's simply not an option in the picker.
5. **Volume buttons**: optional, configurable which direction does what, and there's a dedicated **Lock** toggle (top of the home screen) so BLE/headset/volume buttons can be fully disabled while you're doing manual tapping.
6. **Hold-to-ignore**: any tap (screen or volume key) held longer than the threshold in Settings (default 600ms) doesn't count — prevents accidental multi-counts.

## Building it

You'll need [Android Studio](https://developer.android.com/studio) (free). This project was written as source only — it wasn't compiled here, since building an APK needs the Android SDK/Google's Maven repo, which this sandbox doesn't have access to.

1. Open Android Studio → **Open** → select this `TapCounter` folder.
2. Let Gradle sync (it'll download dependencies the first time — needs internet).
3. Plug in your phone (USB debugging on) or use an emulator, hit **Run**.
4. Grant Bluetooth/notification permissions when prompted.

If Android Studio flags any dependency version as outdated, it's safe to accept its suggested upgrade.

## Connecting your ring

Your ring's firmware needs to send a single-byte GATT **notification** on a characteristic, where the byte value is:
- `0x01` → "Button A"
- `0x02` → "Button B" (defaults to your stealth/negative button)
- `0x03` → "Middle button" (defaults to doing nothing — can't be set to Reset)

In Settings → Custom BLE ring, paste your ring's actual Service UUID and Characteristic UUID (from your existing nRF52840 firmware/Web Bluetooth app), then tap "Scan for ring" and pick your device from the list. Which logical button (A/B/Middle) triggers which action (Increment / Decrement / Stealth toggle / Force stealth on / None) is fully re-orderable there too.

If your firmware sends a different byte scheme, the parsing logic is one small block in `ble/BleTapManager.kt` → `handleRawButtonCode()` — easy to adjust to match.

## Known limitations

- **Foreground notification**: Android requires *some* visible notification for a background service that holds a Bluetooth connection — there's no way to fully hide it at the OS level. It's set to a generic, low-priority "Tap Counter — Connected" notification rather than anything revealing.
- **Screen brightness in stealth mode**: dimmed as low as Android allows (not the physical backlight — some phones have a hardware minimum above true black).
- Icons/launcher art are minimal placeholders (dark red/black, matching your usual notes theme) — swap `res/drawable/ic_launcher_*.xml` for anything nicer.

---
# v2 — Naam Jap additions

Menu (⋮) on the counter screen now has **पाठ संग्रह (Library)** and **Tratak HUD**.

## Library (पाठ संग्रह)
- Sections/titles from your screenshots: Shri Hit Harivansh Vani, Ashtayam Seva, Nitya Path Rasopasana.
- Reader: swipe pages or scroll down, themes + custom colors, font family/size/line spacing/alignment/bold,
  language dropdown (hi/en/gu/bn/mr/ta/te), meaning (अर्थ) on/off, index, refresh (reset look), audio, prev/next.
- **Text content is NOT bundled** (only Chaurashi pad 1 as a sample). Add it with ✎ in the reader:
  paste one pad, or turn on "Bulk paste" and paste many pads separated by a blank line — they are
  numbered consecutively from the pad number you type. Add a translation by switching the language
  dropdown first, then pasting that language's text.
- Audio: put an https audio URL in ✎ → "Audio URL"; it streams (not copied into the app).

## Pad Practice (Chaurashi, Radhasudhanidhi, Sevak Vani)
Pick pads by 1–12, range, grid taps, whole row/column, or random; choose "same pads every day" or
"move on, no repeats in the series" (N per day); optional auto-shuffle. "Start in stealth" = black screen,
tap = haptic + next pad, long-press = exit.

## Learn Pad
Pad number + first line, searchable, tap to open.

## Tratak HUD
Live camera with your custom name over it (size, color, glow, opacity, repeat across screen, slow pulse),
timer, vibrate-every-N-minutes, Stealth (controls hidden, dimmed, touch-locked; long-press to exit).
Preview only — nothing is recorded or saved.

## Build from phone
Push to GitHub as before; Actions builds `app-debug.apk`.
