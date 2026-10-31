# PhiOS Mobile: Rung 2 native Android launcher

**Scope:** An ordinary, user-selectable Android HOME app for test devices and emulator. This project does not replace, unlock, root, flash, modify, or bypass Android firmware.

## Implemented

- Native HOME/DEFAULT intent filter plus independent LAUNCHER entry for opening the app before selecting it as Home
- LauncherApps discovery of current-user launchable activities and launch via startMainActivity
- Filterable app list, icon and label display, pinned favorites stored locally on the device, no contact/SMS/call access
- Long-press any entry to pin/unpin; native PhiSurf radial launcher with six directions and accessible buttons
- System Settings entry, change-default-Home deep link, explicit status/limitations dialog
- No INTERNET, QUERY_ALL_PACKAGES, notification listener, accessibility service, device administrator, or sensitive Android permission
- Unit tests for the query and six-sector PhiSurf mapping
- Java source only, no AndroidX or external runtime libraries

**This is not a production-ready daily driver launcher.** It has not been emulator-tested or qualified on a physical phone. Android owns the lock screen, calls, emergency services, notifications, cellular connectivity, security and boot behavior. Notification integration is a later opt-in milestone, not implemented here.

## Build

Requirements:
- JDK 17
- Android SDK Platform 36 and Build Tools 36.0.0
- Gradle 9.6.0 and Android Gradle Plugin 9.4.0

From repository root:

    gradle --no-daemon -p android :app:assembleDebug :app:testDebugUnitTest

Debug APK output:

    android/app/build/outputs/apk/debug/app-debug.apk

The \`Android Native Launcher\` GitHub Actions workflow builds and tests on GitHub-hosted runners and uploads the **debug APK** as a short-lived Actions artifact if the build succeeds. This artifact is for **testing only**, not a production release or a verified distributable.

## Test without replacing Home

1. Prefer an Android emulator or spare device; preserve a working, familiar Home app.
2. Download the debug APK from a successful GitHub Actions run or build it with Android Studio.
3. Install on an authorized test device: \`adb install -r android/app/build/outputs/apk/debug/app-debug.apk\`.
4. Open "PhiOS Mobile" like any other app without selecting it as default.
5. Confirm real launchable app discovery, filtering, tapping, pinned favorites, native PhiSurf drag, six fallback buttons, accessibility, orientation, and return from opened apps.
6. Only if comfortable, open Android **Settings → Apps → Default apps → Home app** and choose PhiOS Mobile temporarily. Names differ by OEM.
7. To exit, choose the original Home app in the same Android setting. If the launcher crashes, use the device's Settings/recovery workflow or \`adb shell cmd package set-home-activity <stock-component>\` only after verifying your device's official procedure. Avoid installing this on your only emergency-call-capable phone until tested.

## Data and privacy

The only user state stored is a set of pinned component identifiers in private Android SharedPreferences. No contact, message, call, location or telemetry collection. Cloud app backup is opted out through the manifest and extraction rules. This is **not** a validated export/restore scheme. The browser PWA's notes and preferences remain separate: native/web synchronization and import are NOT implemented.

There is no installer, root helper, system image, OTA updater, bootloader step or automatic Home selection.

## Limitations and upcoming work

- No real notification shade or Android notification integration
- No widgets, multi-page launcher editing, native home customization sync, app shortcuts or multiple-profile support
- No app-uninstall or app-permission management
- Needs Android emulator smoke and at least one named spare physical device test before calling the UI qualified
- Release signing, package provenance and independent device compatibility evidence are pending

See [PhiOS Mobile roadmap](../docs/ROADMAP.md). Source identity and UI prototype live in the same repository but are independent outputs.
