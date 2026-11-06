# Φ Universal Source Shelf R3 — Android HOME test launcher

R3 adds **Source Shelf** to the existing user-selectable Android Home **test APK**. This feature is not Android firmware, not a universal APK installer, and not a new permission request.

## Behavior

- The Home **Source Shelf** button opens a separate dialog showing **public GitHub source bookmarks**. These have no relationship to the existing installed Android app pins.
- A bundled, validated, offline catalog contains **225 public repositories** pinned to the same FieldConstellation source snapshot SHA as PhiOS Core and PhiOS Web/Mobile PWA.
- The user can **find and pin by public project name**, reorder, remove, explicitly view a source in an *external browser*, export JSON via Android's document provider, or import after a confirmation preview.
- Bookmark data is stored in private Android SharedPreferences under `portable-source-shelf-r3`; normal Android launcher pinned component IDs remain under `pinned`. The existing layout backup and default-Home recovery continue unchanged.
- Portable JSON is `phios-app-shelf` version 1, 24 unique known source IDs maximum, 16 KiB maximum, fixed snapshot hash, strict UTF-8 and no extra root keys. Imports cannot install packages, access app data, alter Home selection or acquire credentials. The document provider the user chooses may be cloud-backed; for local-only portability choose device storage.
- The Android app itself still requests **no INTERNET permission**. Explicit external source viewing delegates to whatever browser is available; it is not background fetching or app execution.

## Stop/go tests

`gradle -p android :app:testDebugUnitTest :app:assembleDebug` must pass strict codec and existing launcher tests. The Android emulator workflow must continue to show actual installed-app discovery/launch and restoration of the stock HOME. The additional native Source Shelf buttons should be visually inspected for 320 dp width, enlarged text and TalkBack before a *spare* physical phone is attempted. Never select the debug APK as the default HOME on an only/emergency phone.

Native Linux PhiShell GUI integration is in the companion PhiOS R3 PR. This feature does not automatically synchronize source pins across browser origins or devices: export/import is user-selected and explicit.
