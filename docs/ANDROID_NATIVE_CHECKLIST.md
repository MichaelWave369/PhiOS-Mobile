# Rung 2 native launcher qualification checklist

**Never confuse successful compilation with device qualification.** Record API level, Android version, OEM, model, build fingerprint, time, tested APK digest and whether PhiOS Mobile was selected as the default HOME.

## Build proof
- [ ] GitHub CI native assembleDebug and testDebugUnitTest green
- [ ] Build artifact downloaded and SHA-256 recorded
- [ ] APK inspected for package ID, requested permissions and HOME intent

## Android emulator
- [ ] Package installs and opens from LAUNCHER without default-home selection
- [ ] Actual launchable app list populates; search filters; tapping opens a real app
- [ ] Pin and unpin via long-press; pins persist after relaunch and reboot
- [ ] PhiSurf drag works in all six sectors; button fallback works with keyboard/screen reader
- [ ] Rotations and inset layouts work without clipping
- [ ] Default HOME selection works and can be reversed
- [ ] Crash recovery and restoration of original Home documented
- [ ] No notification/call/radio authority implied

## Named physical test device
- [ ] Stock Home retained, ADB/recovery instructions recorded
- [ ] Native launcher test installed and removed successfully
- [ ] Default Home selected and reversed without losing normal access
- [ ] Launch, pinned state, rotation and accessibility tested
- [ ] No claims about emergency calls until separately tested with provider-approved methods

## Future permission-sensitive features
Notification listening, shortcuts and widgets each need separate user permission/consent design and scoped test evidence. No silent broad privileges, root, background polling or phone settings simulation.
