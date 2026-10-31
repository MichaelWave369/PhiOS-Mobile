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

## Rung 2.1 automatic emulator gates (PR under review)
- [ ] Open Layout dialog; hide hero and verify it disappears
- [ ] Force-stop/restart and verify layout setting persisted
- [ ] Reset layout; verify welcome card returns
- [ ] Record current HOME role (including empty) and validate a qualified stock system launcher component
- [ ] Temporarily select PhiOS as default HOME, send Home key, verify PhiOS renders
- [ ] Recover stock HOME, then restore the exact initial role assignment (even if empty); fail CI if either restoration is not confirmed
- [ ] Collect and inspect default-HOME and restored-home screenshots and receipt
- [ ] Never claim physical hardware support from disposable-emulator success

## Rung 2.1b deterministic app-launch gate
- [ ] A standalone, permission-free test Android APK installs alongside the launcher
- [ ] Long-press pinning produces a visible, tappable native Home shelf icon
- [ ] The fixture opens its own foreground Activity when tapped from the all-apps list
- [ ] Return to the PhiOS Activity, pin the fixture and launch again from its shelf icon
- [ ] Force-stop and restart PhiOS; pinned shelf persists
- [ ] Existing layout and Home-role restoration checks still pass
- [ ] Verify the fixture is NOT bundled into or shipped as the PhiOS launcher APK

## Manual emulator follow-up
- [ ] Independently retest default HOME and stock restoration with manual interaction
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

## Rung 2.1c: Post-merge emulator boot reliability

- [ ] Check a clean Android emulator launch at the actual merged commit
- [ ] If the UI is not initially visible, record window/activity snapshots and process state
- [ ] Distinguish background setup/keyguard interference from a PhiOS crash or invisible foreground UI
- [ ] Retry only when PhiOS remains running but another Android surface owns the foreground
- [ ] Fail on missing process, PhiOS-attributed fatal exception, or resumed launcher without UI
- [ ] Record startup attempts, screenshots, and exact APK SHA-256 in the test receipt
- [ ] Verify a green emulator run **after** merging, not only before

## Emulator workflow trigger semantics

The emulator workflow runs for **pull requests** and for **post-merge pushes to main**. It can also be run manually from GitHub Actions. Feature-branch pushes do not launch a second duplicate emulator job when a pull request is already running. This avoids concurrency cancellation of one run by the other.

Check the exact pull-request HEAD SHA for the pre-merge gate and the main merge SHA for the post-merge gate. A cancelled superseded run is not a successful or failed emulator test, and all required completed runs still must pass before device qualification claims.
