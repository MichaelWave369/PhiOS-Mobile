# Security boundary: Rung 1 browser demo

**This is a UI and application-state prototype, not a mobile operating system.**

| Area | Observed/implemented | Not established |
| --- | --- | --- |
| Storage | Note and preferences in origin-local localStorage | Encryption, persistence guarantees, OS backup, multi-device sync |
| Export | Plain JSON export with schema/version, opt-in overwrite on import | Encrypted archive, authenticated provenance, independent restore audit |
| Navigation | Touch buttons, edge gesture, keyboard access | Android gesture integration or system-wide application launcher |
| Offline | Service worker attempts to cache the static site | Offline qualification on actual browsers/devices, guaranteed availability |
| Pulse | Browser reports its own hints and APIs | Cellular connectivity, Internet reachability, firmware or hardware health |
| Permissions | Browser sandbox only | Android permission controller, SELinux policy, broker authority |

Service-worker scope is limited to the app path and same origin. The code uses no analytics, API keys, remote assets, or network upload endpoints. Never import secrets into Notes: both localStorage and JSON downloads are plaintext and may be readable to other software with access to the browser profile or downloads.

No security status from this app should be treated as device attestation. Rung 1 build/test success does not qualify native Android packaging, verified boot, emergency calling, or ROM flashing. Human review and device-specific test evidence remain mandatory.
