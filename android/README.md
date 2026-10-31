# PhiOS Mobile: Rung 2 native Android launcher

**Scope:** An ordinary, user-selectable Android HOME app for test devices and emulator. This project does not replace, unlock, root, flash, modify, or bypass Android firmware.

## Rung 2.1c: Reliable emulator startup verification

The API 35 emulator smoke test now distinguishes first-boot focus delays from genuine PhiOS launcher failures. It wakes the disposable emulator, retries only when PhiOS remains running but is not the resumed foreground Activity, and records Android activity/window diagnostics, screenshots, launcher startup attempts and an APK-identified JSON receipt. A crash, terminated process or resumed launcher without its UI **fails immediately**. It does not hide runtime failures or change physical device behavior.

## Rung 2.2: Native launcher profile export and restore

The **Backup** button on the native Android Home opens **Export to JSON** and **Import from JSON** using the OS document picker. PhiOS does not need an Internet, storage, account, or all-packages permission. The user chooses the destination; a document provider *can* be cloud-backed, so choose on-device storage when local-only retention matters.

The versioned JSON contains only pinned launcher component identifiers and four Home layout switches: `show_clock`, `show_hero`, `compact_rows`, `show_shelf`. It contains no app data, other app inventory, notes, accounts, device identifiers, or system Home preferences. The PWA/PhiOperator backup remains separate.

Imported documents have a 64 KiB hard cap and at most 256 pinned IDs. The codec checks schema/version, field types, valid UTF-8, duplicate IDs, and component identifier format. IDs for apps not installed on the receiving device are **skipped**, never installed or launched. A confirmation preview shows what will be restored, and cancelling leaves the current settings unchanged. Restoration updates only the existing native pins and layout.

The JSON codec has JVM regression tests, and emulator CI verifies the native menu exists. A full OS-document-provider save/open round trip, screen-reader qualification and spare physical-device testing remain pending.

## Rung 2.1b: Pinned Shelf and real launch proof

- A native scrollable **Pinned Shelf** above the app list, populated only from installed launcher activities that the user has pinned
- One-tap launch and long-press unpin from the shelf, with accessible labels and a shortcut to all pinned apps
- At most eight icons on the shelf to bound layout and icon loading; all favorites remain accessible in the pinned app list
- Layout preference to show or hide the shelf, persisted locally
- A separate test-only `fixture` application compiled and installed in disposable emulator CI, with no permissions and a uniquely identifiable foreground screen
- Emulator smoke tests for actual app launch **from the installed-app list and the pinned shelf** and returning to the PhiOS launcher
- The `fixture` APK is never shipped inside the launcher APK and is not uploaded as a deliverable

Installed activities launch through the Android `LauncherApps` API when permitted; if the platform gates that API for a non-default launcher, ΦOS falls back to a standard explicit `ACTION_MAIN` / `CATEGORY_LAUNCHER` intent for that same current-user app activity. Failures remain visible to the user. No broad app, notification or phone permission is requested.

No Android notification, Internet, SMS, call, or system-setting privileges were added. The stock Home recovery test remains mandatory.

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

From repository root (release/testing app only, **not** the fixture):

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

### Test fixture privacy boundary

The `android/fixture` project exists solely for reliable emulator verification. It is a separate app with no permissions, no account, and no data flow to PhiOS. Its `PHIOS CI TARGET LIVE` screen proves that the OS launched a distinct installed Android application. The regular native launcher workflow builds and uploads only `android/app/build/outputs/apk/debug/app-debug.apk`; the emulator workflow additionally builds `fixture:assembleDebug` but does not upload its APK.
