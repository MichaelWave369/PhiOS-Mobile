# Φ PhiOS Mobile

**Sovereign. Coherent. Local. Free.**

PhiOS Mobile extends the PhiOS philosophy to touch-first devices. **Rung 1.1 is a standalone browser/PWA prototype**, not a flashable Android OS, a custom ROM, or evidence of phone hardware compatibility.

## Native emulator qualification (next gate)

The new **Android Emulator Smoke** workflow boots a disposable Android 15 (API 35) virtual phone on GitHub. It builds the APK, installs it, inspects compiled permissions, exercises visible launcher and PhiSurf controls, checks search and pinned shortcuts, and captures screenshots, UI hierarchies and an evidence receipt. This does not require a local Android installation or change any personal phone. A successful run is *emulator-specific*, not proof of physical-device compatibility or emergency service readiness.

See [emulator testing](android/README.md#run-without-installing-anything-locally) and [review checklist](docs/ANDROID_NATIVE_CHECKLIST.md).

## Rung 2: Android native launcher preview

The `android/` subproject is a separate, user-selectable **Android HOME app**. It discovers and opens current-user launchable apps through Android LauncherApps, supports local pinned shortcuts and native PhiSurf, and provides a way to return to the original default Home. This is **not a new Android OS**, custom ROM, or daily-driver qualified launcher.

- Build and test with `gradle --no-daemon -p android :app:assembleDebug :app:testDebugUnitTest`
- GitHub's Android workflow uploads a short-lived **debug test APK** when successful.
- Do not select as your default Home on your only phone before emulator/spare-device testing.
- Native pins and the browser Notes/Vault remain separate. No permission to view notifications or control radios is requested.
- See [native build and rollback instructions](android/README.md) and [device qualification checklist](docs/ANDROID_NATIVE_CHECKLIST.md).

## Rung 1.1: The living shell

- Smooth directional screen transitions, respecting reduced-motion preferences
- Local quick settings drawer via the top button or downward edge swipe; these controls are **not** Android system toggles
- Home editor with accessible move-up/down shortcut reordering, grid/list appearance, optional clock and hero card
- PhiSurf center-to-sector drag selection with visual feedback; tap and keyboard alternatives remain
- Right-edge swipe toward the center to return home; left-edge swipe still opens PhiSurf
- Hidden visual scrollbar and improved scroll padding around the bottom dock
- Existing Rung 1 notes and schema 1 JSON backups migrate into the version 2 local state format

## Rung 1: PhiShell Touch

- Distinctive responsive home screen and six local shell destinations
- **PhiSurf:** radial quick navigation by button, left-edge swipe, or Alt+S; Escape closes
- **Notes:** local scratchpad with no account, backend, or automatic sync
- **Vault:** plaintext JSON export and opt-in restore of notes and settings
- **Settings:** local accent, compact grid, and reduced-motion preferences
- **Pulse:** honest browser hints about network, secure context, service worker, storage, and standalone display
- **Offline-capable PWA:** caches the static shell after a successful initial load and service-worker installation
- Node standard-library unit tests and GitHub Actions verification, with a separate Pages publishing workflow

## Run locally

Serve the folder from a local HTTP server, for example:

    python -m http.server 8080

Then open http://localhost:8080/. The app uses browser modules, so opening index.html directly as a file is not supported.

For verification (Node 20+; no npm dependencies):

    npm run check
    npm test
    npm run build

The deployable static output is in dist/. Run its files on a real HTTP(S) origin. The GitHub Pages workflow publishes dist/ when main changes and the repository is configured to deploy from GitHub Actions.

## Security and portability

Data lives in browser localStorage and is **not encrypted**. Exported backups are readable JSON, not protected archives. Browser storage can be cleared by the browser or device, and is not equivalent to durable, independently tested OS recovery. No cloud is required for the shell, but offline access requires the service worker to install successfully while online and must be tested separately.

This prototype does **not** handle calls, SMS, emergency services, cameras, modem drivers, Android app permissions, verified boot, secure elements, or firmware upgrades. The UI never claims these capabilities.

See [the security boundary](docs/SECURITY_BOUNDARY.md) and [the build roadmap](docs/ROADMAP.md).

## Relationship to PhiOS

The desktop PhiOS repo is a separate alpha system with explicit authority and release controls. Mobile shares its **principles** but does not automatically inherit its implementations, test evidence, release qualifications, or security authority.

Source: [PhiOS](https://github.com/MichaelWave369/PhiOS) · [Sovereignty Contract](https://github.com/MichaelWave369/PhiOS/blob/main/SOVEREIGNTY_CONTRACT.md).

## License

MIT for repository-authored code. Third-party device/vendor components in future Android builds retain their own licenses; no Android source or proprietary blobs are included here.
