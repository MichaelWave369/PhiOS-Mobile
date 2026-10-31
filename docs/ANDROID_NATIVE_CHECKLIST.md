# Rung 2 emulator and physical-device qualification

## Disposable GitHub-hosted emulator (proposed CI)
- [ ] Android debug APK compiles, installs, launches, and shows real app list
- [ ] Compiled permission report excludes Internet and sensitive phone/data privileges
- [ ] PhiSurf six-sector UI opens; text-button fallback and status panel respond
- [ ] Search returns a no-results state for a nonsense query
- [ ] Long-press pin persists across activity process restart
- [ ] Artifacts include PNG captures, UI XML and APK SHA-256
- [ ] Screenshot captures reviewed manually for clipping, legibility and gesture problems

The emulator gate explicitly does **not** change the default HOME activity. It tests neither voice service reliability nor platform recovery. An emulator pass is narrow evidence, not device certification.

## Manual emulator follow-up
- [ ] Test choosing PhiOS Mobile as default HOME and safely reverting to stock
- [ ] Rotate and resize; verify layout, keyboard and dock interactions
- [ ] Real app-launch and return from another app
- [ ] TalkBack screen reader, physical keyboard, focus order, touch targets
- [ ] Empty, disconnected and disabled-app scenarios
- [ ] Confirm recovery after force-stop and configuration changes

## Specifically named physical spare device
- [ ] Save stock launcher identity, Android version and complete backup
- [ ] Test installing/uninstalling, installed app discovery and foreground launch
- [ ] Test default HOME change and restoration with hardware/vendor instructions
- [ ] Test pin/search/rotation/accessibility and reinstall data behavior
- [ ] Inspect APK permissions and record exact SHA-256/device fingerprint
- [ ] Do not test real emergency dialing through synthetic methods
- [ ] Keep daily-driver and emergency calling qualification explicitly pending

Use [the native launcher README](../android/README.md) for installation and scope.
