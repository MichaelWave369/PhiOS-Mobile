# Rung 4 / PhiOperator Passport, browser PWA only

The **separate native Android launcher is not changed by this integration.**
The web app provides an explicit user-controlled import for the PhiOperator v1
JSON file. This is local, optional and reversible.

Protocol source: PhiOperator src/portable-passport.js, pinned from commit
402aa3b161737926617d578370749826e3fda97a.
Vendored offline as src/operator-passport.mjs (identical source).
The PWA's build script copies src/, and the service worker precaches this module.

To use: clear Standard Rank 2 at PhiOperator, export your v1 Passport,
then PhiOS Mobile PWA → Settings → Operator Passport → Import. After
confirmation, a decorative home halo appears. Export a copy or remove it
from Settings anytime. The Notes/Vault backup is separate and unchanged.

No cross-site localStorage reads or cookies; no accounts, server, auth token,
privileged OS methods, or network access introduced. The plain JSON is
user editable and must NEVER grant real security permissions or access to core
functions. The full PWA works without any imported Passport.

Verification: npm run check && npm test && npm run build.
Test blocked storage, malformed files, repeated import/removal, persistence,
offline install/cache upgrade, reduced motion, and mobile browser appearance.
The separate native Android emulator qualification is unaffected.