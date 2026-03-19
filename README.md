<div align="center">

# Snap Tiles

<img src="media/ic_app_logo_round.webp" alt="App Logo" width="128"/>

[![en](https://img.shields.io/badge/lang-en-red.svg)](README.md)
[![vi](https://img.shields.io/badge/lang-vi-green.svg)](README.vi.md)

Quick Settings tiles to toggle system settings on Android with a single tap — no need to open the Settings app.

</div>

## Features

- **Fixed Tiles** — USB Debugging, Developer Mode, Accessibility (always enabled)
- **Custom Tiles** — Up to 5 configurable slots with multiple actions per tile
- **Floating Tile Button** — Draggable overlay button, snaps to edge, long-press to open app
- **Smart Caching** — Remembers Accessibility services and USB state, restores on re-enable
- **System Controls** — Stay Awake, Running Services, Force RTL Layout
- **Advanced Debugging** — Profile GPU Rendering, Demo Mode, Animator Duration Scale

## Latest Updates

- **Home Screen Widgets & Shortcuts**: Implemented new home screen widgets and enhanced app shortcuts for quicker access to features.
- **General Improvements**: Various updates and optimizations.
- **New Release**: Version 1.0.2 (build 3) is now available.

## Screenshots

| Floating Button | Snap to Edge | Custom Tiles |
|---|---|---|
| ![Floating Button](media/floating-snap.jpeg) | ![Snap to Edge](media/floating-snap-visual.jpeg) | ![Custom Tiles](media/multiple-and-custom-tile.jpeg) |

## Demo

![Demo](media/howtouse.gif)

## Installation

### Download APK

Grab the latest release from the [Releases page](https://github.com/kaidraw-21/android-snap-tiles/blob/main/RELEASES.md).

**[⬇ Download snap-tiles-v1.0.2.apk](https://github.com/kaidraw-21/android-snap-tiles/raw/main/download/snap-tiles-v1.0.2.apk)**

### Build from source

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## ADB Setup

### Enable Developer Options and USB Debugging

Before proceeding, ensure you have Developer options and USB debugging enabled on your Android device.

1.  **Enable Developer Options**:
    *   Go to `Settings` > `About phone`.
    *   Tap `Build number` 7 times until you see a message "You are now a developer!" or "Developer options enabled".
2.  **Enable USB Debugging**:
    *   Go to `Settings` > `System` > `Developer options` (or `Settings` > `Developer options`).
    *   Turn on `USB debugging`.

Grant the required permission once via ADB:

```bash
adb shell pm grant com.snap.tiles android.permission.WRITE_SECURE_SETTINGS
```

Then pull down Quick Settings → Edit (pencil icon) → drag the tiles you want into your panel.

## Notes

- Permission `WRITE_SECURE_SETTINGS` is required once via ADB. No ADB needed after that.
- No root required.
- Fixed tiles are always enabled — no on/off switch.
- Accessibility tile caches active services and restores them on re-enable.

## Changelog

See [RELEASES.md](RELEASES.md) for full release notes.
