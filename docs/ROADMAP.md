# PhiOS Mobile: milestone ladder

## Rung 1: Touch shell (this PR)
- [x] Responsive, touch-friendly browser shell
- [x] PhiSurf navigation with accessible button alternatives
- [x] Local notes, preferences, export, and import
- [x] Static PWA cache and offline-support infrastructure
- [x] Truthful browser capability/status screen
- [x] Testable core logic and automated static build
- [ ] GitHub Actions CI pass on the PR (verify after GitHub runs)
- [ ] Manual phone-browser and keyboard/screen-reader smoke test
- [ ] Prove offline reload after installation on actual target browser
- [ ] Enable and verify GitHub Pages deployment

## Rung 2: Android launcher prototype
- Make an Android application that can be selected as the device launcher.
- Implement native app discovery, system intents, and notifications using user-granted Android APIs, not simulated privileges.
- Keep launcher packaging separate from AOSP device images.
- Test on emulator and one specifically named physical device; preserve stock recovery path.
- Never claim calls, emergency support, or permission isolation from the web demo.

## Rung 3: Device-targeted AOSP image
- Choose one unlockable, well-supported reference device, verifying bootloader rules and official factory-image restore.
- Produce a reproducible build and hardware license/SBOM inventory.
- Qualify verified boot state, OTA signing, encryption, recovery, cameras, Wi-Fi, Bluetooth, cellular/IMS/VoLTE/SMS, GPS, battery/thermal, accessibility, banking limitations, and emergency calling as separate gates.
- No daily-driver recommendations until emergency calling, rollback, and hardware recovery are independently demonstrated.
- Never ship unreviewed proprietary components or claim hardware support by analogy.

## Rung 4: Unified PhiOS ecosystem
- Shared open data schemas between desktop, mobile, XR, and local home hosts.
- Optional explicit pairing; no required Phi account or recurring cloud token.
- PhiServe / PhiStream optional processing with clear device-side consent and revocation.
- Portable backups and independent restoration tests.

## Governance
Follow the PhiOS Sovereignty Contract: user exit, offline behavior, model/provider choice, permission boundaries, and truthful evidence. Labels like verified, secure, installed, and supported require concrete device-bound test receipts. This roadmap is a plan, not qualification evidence.
