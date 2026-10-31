# PhiOS Mobile: milestone ladder

All statuses below describe narrow engineering evidence. CI compile success does not mean a device or operating system is qualified.

## Rung 1: Touch browser shell (merged)
- [x] Touch-first browser home, notes, vault, PhiSurf and settings
- [x] Static site build, unit tests and GitHub Pages workflow
- [x] CI checks passed on PR #1
- [ ] Physical phone and screen-reader interaction review
- [ ] Confirm offline reload on supported physical browsers

## Rung 1.1: Living touch shell (merged)
- [x] Reorder shortcuts, choose grid/list, hide home panels
- [x] PhiSurf dial, local quick controls, scoped edge gestures and transition polish
- [x] Backward-compatible storage and JSON backup migration
- [x] CI passed on PR #2
- [ ] Manual screen-reader and physical mobile-browser UX qualification

## Rung 2: Native Android HOME launcher (merged)
- [x] Separate native Java launcher with HOME and LAUNCHER intents
- [x] Real launchable app list, app launch, search and pinned shortcuts
- [x] Native PhiSurf dial with accessible button choices
- [x] No internet, all-packages, notification-listener or call permissions
- [x] Debug APK built; JVM tests passed on PR #3
- [ ] Android emulator launcher UX qualification
- [ ] Default HOME choice and rollback qualification
- [ ] Spare physical device smoke testing and accessibility audit

## Rung 2 verification: disposable emulator (this PR)
- [x] Add CI that boots a virtual Android phone without local setup
- [x] Add black-box APK install, permission, app list, PhiSurf, search and pin checks
- [x] Capture screenshots, XML trees, relevant logs and APK-identified test receipt
- [ ] Run CI successfully and inspect screenshot artifacts
- [ ] Test real HOME selection/rollback in a separate controlled test
- [ ] Test a specifically named spare physical device

## Rung 2.1a: Native layout and reversible HOME gate (this PR)
- [x] Local native layout preferences for clock, welcome card and compact app rows
- [x] Emulator black-box tests for layout persistence and reset
- [x] Emulator HOME selection and mandatory stock HOME restoration test
- [ ] Review green emulator run, screenshot artifacts and receipt for this exact commit
- [ ] Real-device testing and accessibility remain unqualified

## Rung 2.1b: Pinned Shelf and real Android app launch (this PR)
- [x] Add an accessible horizontal pinned-app shelf to the native Home
- [x] Add local Layout toggle for shelf, launch and unpin interactions, and bounded shortcut count
- [x] Add pure Java shelf-selection tests
- [x] Add test-only Android fixture app and wire only the emulator build to compile it
- [x] Emulator script tests real installed-app launch and return via list and shelf
- [ ] PR CI green including real app launch and default-Home rollback
- [ ] Review real emulator screenshots, focus and viewport clipping
- [ ] Physical spare-device and screen-reader qualification remain pending

## Rung 2.2: Sovereign native profile backup (this PR)
- [x] Add versioned JSON profile for pinned apps and four native Home layout flags
- [x] Export/import through Android's document picker without new permissions
- [x] Strict UTF-8, version, schema, field and component-key validation; 64 KiB limit
- [x] Skip unavailable apps; preview and require confirmation before replacing settings
- [x] Keep Android default Home role, other app data and the PWA vault untouched
- [x] JVM codec security/round-trip tests and emulator backup-menu smoke
- [ ] Verify CI and inspect actual emulator menu screenshot
- [ ] Test full document-provider file round trip and screen-reader interaction
- [ ] Test a specifically named spare physical device

## Rung 2.3: Document provider integration verification (this PR)
- [x] Test native export through the real Android ACTION_CREATE_DOCUMENT picker
- [x] Read saved local JSON document and validate exact portable schema and pinned test app
- [x] Modify native pins/layout and test cancelled import leaves settings unchanged
- [x] Confirm import through ACTION_OPEN_DOCUMENT restores pins/layout after process restart
- [x] Preserve default Home restoration, fixture launch and permission gates
- [ ] PR green emulator run and review actual exported JSON and screenshots
- [ ] Confirm post-merge emulator CI and physically test a spare device
- [ ] Test alternate document providers, accessibility and malformed-file import UI

## Rung 2.4: Auditable debug APK test handoff (this PR)
- [x] Validate package ID, version, min/target SDK and launchable Activity against source and compiled manifest
- [x] Fail on any requested Android permissions
- [x] Verify Android debug signature and record signer certificate fingerprint
- [x] Upload one checksum-verifiable test bundle with APK, JSON receipt, SHA256SUMS and safety instructions
- [x] Include Python regression tests for metadata, permissions and signature parsing
- [ ] CI green and inspect receipt from the exact built commit
- [ ] Manual spare-phone install, update, original Home restore and uninstall
- [ ] Separate secure *release signing* and reproducible build strategy; debug APK is not a production release

## Rung 2.1: Native UX and explicit optional capabilities
- Enhance native home layout, widgets, shortcuts and bounded quick controls
- Consider notification access only with independent opt-in and revoke instructions
- Add a signed release track and independently reproducible APK build evidence
- Keep native app preferences separate from browser notes until a reviewed migration/sync mechanism exists

## Rung 3: Device-targeted AOSP image
- Select one officially unlockable reference device with documented stock restoration
- Build reproducibly from reviewed sources, licenses and vendor dependency inventory
- Qualify boot, updates, storage encryption, accessibility, battery, cellular, SMS, calls,
  emergency calling and hardware services separately before any daily-driver claim
- No firmware flashing or support claims from emulators alone

## Rung 4: Unified sovereign ecosystem
- Versioned portable data schemas across desktop, mobile and XR
- Optional consent-based local host pairing and provider-neutral inference
- Independent local backups, exit tests, restore and rollback evidence

Source principle: capability is not authority. A green badge must always name exactly what was tested.
