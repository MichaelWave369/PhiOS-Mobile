# Φ PhiOS Mobile

**Sovereign. Coherent. Local. Free.**

PhiOS Mobile extends the PhiOS philosophy to touch-first devices. **Rung 1 is a standalone browser/PWA prototype**, not a flashable Android OS, a custom ROM, or evidence of phone hardware compatibility.

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
