# PhiOS Mobile: Rung 2 native Android launcher

**Scope:** An ordinary, user-selectable Android HOME app for test devices and emulator. This project does not replace, unlock, root, flash, modify, or bypass Android firmware.

## Rung 2.1 additions

- **Layout** action within the native launcher: show/hide clock, show/hide welcome card, and compact app rows
- Native preferences persist after closing/restarting the app; no web/backend synchronization
- Emulator CI tests the layout dialog, persistence and reset
- Emulator CI additionally switches to PhiOS as temporary default HOME and recovers a verified stock HOME launcher, then restores the original role state (which can be empty on a fresh emulator); a failed restoration fails the job
- Default HOME switching remains **manual** on physical devices, never performed by the installed app silently

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


## Run without installing anything locally

Open **Actions → Android Emulator Smoke** in the PhiOS-Mobile GitHub repository. It runs automatically for emulator-related changes and can be invoked with **Run workflow**. The job starts a disposable Pixel-profile Android 15 (API 35) emulator on a GitHub-hosted runner. It installs the APK, uses the Android UI hierarchy to verify launcher rendering and search, opens the native PhiSurf selector and status, and checks local pin persistence across process restart.

The workflow uploads `phios-mobile-emulator-api35-evidence`: actual PNG screenshots, XML UI snapshots, recent logcat, package/activity reports, compiled APK permissions and a JSON receipt tied to an APK SHA-256 digest. A red run is evidence of a failed check, not proof that the launcher is universally broken. Browser PWA testing, screen-reader review, physical Android hardware and HOME-default rollback are separate, pending gates.

Never claim device compatibility from a CI build or emulator pass. The workflow temporarily selects PhiOS as default Home **only on the disposable emulator**, then restores the original role assignment, including an initially empty assignment. It does not perform an emergency-call test.

### Fresh emulator without a default HOME role

Android API 35 virtual devices may start with no HOME role holder. The smoke gate verifies a stock system launcher candidate before mutating anything. When the role is empty, it temporarily seeds a known stock launcher, switches to PhiOS and back, then clears the role to return to the initial empty assignment. Both stock recovery and original role-state restoration must pass, with evidence in the JSON receipt. This behavior is restricted to the disposable emulator and does not imply that a user's device will have an unassigned Home app.
