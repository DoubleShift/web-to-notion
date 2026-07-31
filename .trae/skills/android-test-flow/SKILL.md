---
name: "android-test-flow"
description: "Guide and execute the web-to-notion Android end-to-end test flow. Invoke when the user wants to test the Android APK, verify drawer navigation, or debug Notion sync."
---

# Android Test Flow for web-to-notion

This skill defines the end-to-end test workflow for the web-to-notion Android app. It combines GitHub Actions artifact downloads, ADB installation, automatic Notion credential injection, screenshot verification, and logcat analysis.

## When to invoke

- The user says "test the APK", "run test flow", "verify the app", or similar.
- Need to confirm the custom drawer settings item navigates correctly.
- Need to collect logs for "connected but not syncing" issues.

## Prerequisites

1. An Android device connected via USB with USB debugging enabled.
2. `adb` available at `D:\Dev\Lib\adb\adb.exe` or in PATH.
3. `scripts/secrets.ps1` exists with `$Global:NotionToken` and `$Global:DatabaseId`.
4. `gh` CLI authenticated to access `DoubleShift/web-to-notion`.

## Test script

Run the automated test flow from the repo root:

```powershell
.\scripts\test-flow.ps1
```

Optional flags:

```powershell
# Use a local APK instead of downloading artifact
.\scripts\test-flow.ps1 -ApkPath "D:\apk\app-release.apk"

# Keep downloaded APK and screenshots after the run
.\scripts\test-flow.ps1 -KeepArtifacts
```

## What the script does

1. Downloads the latest successful GitHub Actions APK artifact (`web-to-notion-apk`).
2. Uninstalls any previous build and installs the new APK.
3. Starts the app and broadcasts Notion Token / Database ID into the app via `ConfigReceiver`.
4. Captures a screenshot of the home screen.
5. Taps the drawer menu button and then the "Settings" item, capturing a screenshot of the settings screen.
6. Starts `adb logcat` filtering for the app package and collects logs for 10 seconds.
7. Prints paths to the APK, screenshots, and log file for manual verification.

## Manual verification checklist

- [ ] Home screenshot shows "我的便签" title and no crash.
- [ ] Settings screenshot shows the Settings screen (confirms drawer click works).
- [ ] Log file contains no `FATAL EXCEPTION` or `AndroidRuntime` errors.
- [ ] Log file shows successful Notion API query when testing connection in Settings.
- [ ] After creating a test note, log shows `SaveNoteWorker` running and a successful Notion page creation.

## Coordinates note

The script uses hard-coded tap coordinates (`120 160` for menu, `400 900` for Settings) suitable for common 1080p phones. If the device resolution differs, update these coordinates in `scripts/test-flow.ps1` or use `adb shell wm size` to adjust.

## Related files

- `scripts/setup-device.ps1` — installs APK and injects credentials only.
- `scripts/secrets.template.ps1` — template for local credentials.
- `scripts/secrets.ps1` — ignored by git; stores real Token/Database ID.
- `.github/workflows/build-apk.yml` — produces the APK artifact.
