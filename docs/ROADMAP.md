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
