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
  assert.match(sw, /phios-mobile-operator-r4-v1/);
  assert.match(sw, /src\/living\.mjs/);
  assert.match(sw, /src\/operator-passport\.mjs/);
  assert.match(built, /import \{ initLivingShell \} from "\.\/living\.mjs"/);
});
