import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
const root = new URL("../", import.meta.url);

test("living shell includes unique accessible control IDs", async () => {
  const html = await readFile(new URL("index.html", root), "utf8");
  const expected = [
    "quick-panel", "open-quick", "close-quick", "quick-compact", "quick-motion",
    "home-editor", "app-grid", "home-shortcuts-list", "customize-home",
    "clock-toggle", "hero-toggle", "phisurf", "dial-selection", "screens"
  ];
  for (const id of expected) {
    const count = [...html.matchAll(/\bid="([^"]+)"/g)].filter(match => match[1] === id).length;
    assert.equal(count, 1, "expected one #" + id);
  }
  assert.match(html, /id="quick-panel"[^>]*role="dialog"[^>]*aria-modal="true"/);
  assert.match(html, /id="phisurf"[^>]*role="dialog"[^>]*aria-modal="true"/);
});

test("home reordering targets all six shortcuts, no duplicates", async () => {
  const html = await readFile(new URL("index.html", root), "utf8");
  const ids = [...html.matchAll(/class="app-tile" data-shortcut="([^"]+)"/g)].map(x => x[1]);
  assert.deepEqual(ids, ["notes", "vault", "pulse", "settings", "about", "phisurf"]);
});

test("offline cache has the new interaction module", async () => {
  const sw = await readFile(new URL("sw.js", root), "utf8");
  const built = await readFile(new URL("src/main.js", root), "utf8");
  assert.match(sw, /phios-mobile-dual-track-r24-v1/);
  assert.match(sw, /src\/living\.mjs/);
  assert.match(sw, /src\/operator-passport\.mjs/);
  assert.match(built, /import \{ initLivingShell \} from "\.\/living\.mjs"/);
});


test("Pages distinguishes browser shell from independently tested native Android", async () => {
  const html = await readFile(new URL("index.html", root), "utf8");
  assert.match(html, /WEB DEMO 1\.1 \+ ANDROID LAUNCHER 2\.4/);
  assert.match(html, /id="native-track-title"/);
  assert.match(html, /data-open="about">Explore Android progress/);
  assert.match(html, /id="browser-track-heading"/);
  assert.match(html, /id="android-track-heading"/);
  assert.match(html, /API 35 emulator tests/);
  assert.match(html, /Debug test APK only/);
  assert.match(html, /Not a flashable ROM/);
  assert.match(html, /actions\/workflows\/android-native\.yml/);
  assert.match(html, /docs\/ANDROID_APK_TESTING\.md/);
  assert.doesNotMatch(html, /Android distribution<\/span><strong>Not built/);
  const links = [...html.matchAll(/href="(https:\/\/github\.com\/MichaelWave369\/PhiOS-Mobile[^"]*)"/g)];
  assert.ok(links.length >= 4, "expected native CI, source and safe testing links");
});

test("PWA updates deployed HTML while retaining offline navigation fallback", async () => {
  const sw = await readFile(new URL("sw.js", root), "utf8");
  assert.match(sw, /event\.request\.mode === "navigate"/);
  assert.match(sw, /const response = await fetch\(event\.request\)/);
  assert.match(sw, /cache\.put\(new URL\("\.\/index\.html", scope\), response\.clone\(\)\)/);
  assert.match(sw, /caches\.match\(new URL\("\.\/index\.html", scope\)\)/);
});
