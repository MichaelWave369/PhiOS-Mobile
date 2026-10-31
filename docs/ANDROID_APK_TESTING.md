# PhiOS Mobile native APK: safe test handoff

## What this is

GitHub Actions builds a **debug-signed Android launcher test APK**. This is not a production release, a custom ROM, an AOSP firmware image, or evidence that any physical device is qualified. Use an emulator or **spare Android test phone**, not a phone you rely on for urgent calls.

The \`Android Native Launcher\` workflow produces an artifact named \`phios-mobile-native-debug-test-bundle\` containing:

- \`app-debug.apk\`: the exact APK built by CI
- \`receipt.json\`: package ID, version, min/target Android SDK, launchable Activity, signing-certificate SHA-256 fingerprint, permissions, exact APK SHA-256, CI commit/run identifiers, and explicit test-only status
- \`SHA256SUMS\`: SHA-256 of the included APK
- \`README.txt\`: safety and rollback information

Both the separate legacy debug-APK artifact and the new evidence bundle are temporary GitHub Actions artifacts, **not a signed release**, installer or app-store package. Successful CI means a narrow build/emulator gate was passed, not compatibility with every Android phone.

## Download and validate

1. Open the repository's **Actions → Android Native Launcher** and choose a successful run on \`main\`.
2. Download and extract \`phios-mobile-native-debug-test-bundle\`.
3. Verify \`SHA256SUMS\` against \`app-debug.apk\` (PowerShell on Windows: \`(Get-FileHash .\app-debug.apk -Algorithm SHA256).Hash\`; compare it with the hex digest in \`SHA256SUMS\`). On Linux/macOS run \`sha256sum -c SHA256SUMS\` inside the extracted directory.
4. Compare that digest against \`receipt.json\` and inspect \`android.packageId\`, \`android.versionName\`, \`signature.sha256\` and \`requestedAndroidPermissions\`.
5. Do **not** install if the hash doesn't match, the package ID differs from \`org.enterthefield.phiosmobile\`, or the permissions are unexpectedly nonempty.

The workflow also checks the ZIP archive, APK manifest, package and version against Gradle, launcher Activity, Android permissions, and the APK's **Android Debug** signature using Android's \`apksigner\`. The displayed signing-certificate fingerprint identifies this test APK's certificate; it does **not** establish an independently trusted release publisher or bit-for-bit reproducible builds.

## Test safely and recover

- **Before installing:** use a disposable emulator or a spare phone with a working original Home app; preserve any personal data and the installed stock Home option.
- Install with Android's normal app installation flow or \`adb install -r app-debug.apk\` on an authorized test device. Don't root or unlock a bootloader.
- Launch PhiOS Mobile from the app drawer before deciding whether to change the Home setting. Try pinned apps, PhiSurf and local backup, then verify a restart.
- To switch back, go to **Android Settings → Apps → Default apps → Home app** and select your original launcher. Uninstall PhiOS Mobile only after confirming stock Home functions.
- **Upgrade warning:** GitHub-hosted runs may use different automatically generated Android debug signing keys. An APK from a different run may not install over the previous one. Back up local launcher settings to a file first; on a spare device you may need to uninstall the previous debug build (which deletes its local preferences) before installing the new one. Never bypass Android signature checks.

## Unqualified work

A distributable signed release needs a persistent, protected release signing key, controlled build/release approvals, upgrade and rollback tests, independent verification, accessibility checks, and real-device qualification. None of those are established by this debug handoff. No signing key or signing secret is stored in this repository.
