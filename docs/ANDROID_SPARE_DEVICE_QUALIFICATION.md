# PhiOS Mobile Rung 2.5: Spare-device qualification protocol

**Status: NOT yet qualified on a physical phone.** This is a manual acceptance plan, not evidence that a particular handset works. PhiOS Mobile at this stage is a third-party, user-selectable Android **HOME launcher** and a separate browser/PWA demo, not a custom ROM, replacement cellular stack, or verified emergency-calling platform.

## Stop/go prerequisites

- [ ] Obtain permission to test a **spare** Android device. Never use the only phone available for emergency calls or urgent communications.
- [ ] Record the **manufacturer, model, Android version/build, screen size, system font scale**, and whether a stock launcher is present.
- [ ] Confirm a reliable way to open Android **Settings → Apps → Default apps → Home app** while PhiOS is not selected, using that device manufacturer's instructions. Take a screenshot or note the original Home-app name and icon.
- [ ] Back up important device content independently. Understand that uninstalling the debug APK deletes PhiOS's own private pinned-app/layout settings.
- [ ] Download the **green \`main\` build** from Actions → Android Native Launcher. Choose the \`phios-mobile-native-debug-test-bundle\` artifact and inspect \`receipt.json\`, \`SHA256SUMS\` and \`README.txt\`.
- [ ] Verify \`SHA256SUMS\` matches the bundled \`app-debug.apk\`. Confirm the package ID is \`org.enterthefield.phiosmobile\`, requested permissions are empty, signer is **Android Debug**, and receipt says \`releaseQualified=false\`. See [APK testing guide](ANDROID_APK_TESTING.md).
- [ ] **Stop** if the device is managed by an employer, restricts installing test apps, has an unknown stock Home, or cannot reliably return to stock Home.

## Phase A: Ordinary app, stock Home unchanged

Record PASS / FAIL / NOT TESTED and screenshots (avoid personal information):

| Case | Expected result | Result |
| --- | --- | --- |
| A1: Install the verified debug APK | Android accepts package without new special permissions | NOT TESTED |
| A2: Open PhiOS Mobile from the app drawer | Native Home preview renders, no crash | NOT TESTED |
| A3: Return to stock Home with the Home gesture | Stock Home is unchanged and still operates | NOT TESTED |
| A4: Search installed apps and open a harmless app | Android actually opens the selected app | NOT TESTED |
| A5: Tap **Pin/Unpin** button (not long press) | Pinned shelf and app state update; action is labeled for accessibility | NOT TESTED |
| A6: Test small screen / font scaling / rotation | All apps, PhiSurf, Home help and Backup remain reachable via scrolling | NOT TESTED |
| A7: Explore with TalkBack and hardware keyboard | Logical focus order, labeled controls, no trapped focus or required long-press | NOT TESTED |
| A8: Open **Home help** | Recovery instructions and explicit **Open Home settings** action work | NOT TESTED |
| A9: Export a JSON backup to on-device storage, change layout, restore | Import preview, cancellation and confirmed restore behave as described | NOT TESTED |

If A1–A9 have unresolved failures, **do not make PhiOS the default Home app**.

## Phase B: Optional controlled Home selection

Only on the spare device with a verified stock launcher and approved rollback path.

| Case | Expected result | Result |
| --- | --- | --- |
| B1: Record exact original Home package/app selection | Original choice documented | NOT TESTED |
| B2: Manually select PhiOS under Android's Home-app settings | User explicitly approves; Android presents PhiOS | NOT TESTED |
| B3: Press Home, sleep/wake, return from another app | PhiOS works without interfering with lock screen or system operations | NOT TESTED |
| B4: In PhiOS open **Home help → Open Home settings** | Android Home selector opens | NOT TESTED |
| B5: Choose original Home, press Home | Verified original launcher returns | NOT TESTED |
| B6: Uninstall PhiOS only **after** B5 passes | Original Home keeps working; local PhiOS preferences are removed | NOT TESTED |

**Stop on any unexplained system behavior.** Do not root the phone, unlock its bootloader, modify firmware, bypass a signature mismatch, or attempt to exercise real emergency services as a test.

## Test record

- Tester and consent: ______
- Date/time and time zone: ______
- Device manufacturer/model: ______
- Android version/build fingerprint: ______
- Screen display size, density and font scale: ______
- Original Home name (and verified restore method): ______
- APK SHA-256 from \`SHA256SUMS\`: ______
- Signing certificate SHA-256 from \`receipt.json\`: ______
- CI workflow run URL and exact commit: ______
- Issues and screenshots, with sensitive data redacted: ______
- Phase A outcome: NOT TESTED / PASS / FAIL
- Phase B outcome: NOT TESTED / PASS / FAIL
- Whether stock Home was **confirmed restored**: NOT TESTED / YES / NO
- Tester signoff and next action: ______

Even a fully passing spare-device run is **one specific device configuration**, not certification for other devices, Android updates, critical calling, accessibility across all assistive technologies, or production release signing. Never describe emulator-only results as physical-device verification.

## Emulator versus phone

GitHub's API 35 disposable emulator checks accessible pin controls, a Home recovery guide, a narrow/large-font rendering configuration, and existing save/restore + stock Home rollback. Those checks supply automation receipts, not substitute manual phone testing, a TalkBack audit, or manufacturer-specific compatibility evidence.
