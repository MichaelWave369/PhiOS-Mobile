import test from "node:test";
import assert from "node:assert/strict";
import {
  STORAGE_KEY, APPS, defaultState, sanitizeState, loadState, persistState,
  serializeBackup, parseBackup, NOTE_LIMIT, HOME_SHORTCUTS, normalizeHomeOrder, moveShortcut, wheelSelection
} from "../src/core.mjs";

function storage() {
  const data = new Map();
  return {
    getItem: (key) => data.has(key) ? data.get(key) : null,
    setItem: (key, value) => data.set(key, String(value)),
    removeItem: (key) => data.delete(key)
  };
}

test("six unique app destinations match the shell", () => {
  assert.equal(APPS.length, 6);
  assert.equal(new Set(APPS.map((app) => app.id)).size, 6);
  assert.deepEqual(APPS.map((app) => app.id), ["home", "notes", "vault", "pulse", "settings", "about"]);
});

test("new browser starts with safe defaults", () => {
  assert.deepEqual(loadState(storage()), defaultState());
  assert.deepEqual(loadState(null), defaultState());
});

test("settings and note survive reload from local storage", () => {
  const store = storage();
  const state = { ...defaultState(), note: "A sovereign idea", accent: "violet", compact: true };
  assert.equal(persistState(store, state), true);
  assert.deepEqual(loadState(store), state);
  assert.ok(store.getItem(STORAGE_KEY));
});

test("corrupted storage and blocked storage fall back without crashing", () => {
  const store = storage();
  store.setItem(STORAGE_KEY, "{");
  assert.deepEqual(loadState(store), defaultState());
  assert.deepEqual(loadState({ getItem() { throw new Error("blocked"); } }), defaultState());
  assert.equal(persistState({ setItem() { throw new Error("blocked"); } }, defaultState()), false);
});

test("untrusted settings values are constrained and extra fields discarded", () => {
  const data = sanitizeState({ note: "safe", accent: "malicious", compact: "yes", reducedMotion: true, secret: "unwanted" });
  assert.deepEqual(data, { ...defaultState(), note: "safe", reducedMotion: true });
  assert.equal(Object.hasOwn(data, "secret"), false);
  assert.equal(sanitizeState({ note: "x".repeat(NOTE_LIMIT + 3) }).note.length, NOTE_LIMIT);
});

test("backup round trips human-readable note and preferences", () => {
  const before = { ...defaultState(), note: "Hello Φ", accent: "amber", reducedMotion: true };
  const json = serializeBackup(before, new Date("2026-10-31T00:00:00.000Z"));
  assert.match(json, /Hello Φ/);
  assert.match(json, /2026-10-31T00:00:00.000Z/);
  assert.deepEqual(parseBackup(json), before);
});

test("invalid, unknown-version, malformed, and oversized backups are rejected", () => {
  assert.throws(() => parseBackup("not JSON"), /valid JSON/);
  assert.throws(() => parseBackup(JSON.stringify({ format: "phios-mobile-backup", schema: 3, data: defaultState() })), /not a supported/);
  assert.throws(() => parseBackup(JSON.stringify({ format: "wrong", schema: 1, data: defaultState() })), /not a supported/);
  assert.throws(() => parseBackup(JSON.stringify({ format: "phios-mobile-backup", schema: 1, data: { note: "x".repeat(NOTE_LIMIT + 1) } })), /exceeds/);
  assert.throws(() => parseBackup("x".repeat(1024 * 1024 + 1)), /under 1 MB/);
  assert.throws(() => parseBackup("{}"), /not a supported/);
});


test("pre-existing schema 1 data migrates without losing note or preferences", () => {
  const old = { schema: 1, note: "Keep this.", accent: "violet", compact: true, reducedMotion: false };
  const store = storage();
  store.setItem(STORAGE_KEY, JSON.stringify(old));
  const migrated = loadState(store);
  assert.equal(migrated.note, old.note);
  assert.equal(migrated.accent, old.accent);
  assert.equal(migrated.compact, true);
  assert.equal(migrated.schema, 2);
  assert.deepEqual(migrated.homeOrder, [...HOME_SHORTCUTS]);
  assert.equal(migrated.homeLayout, "grid");
});

test("schema 1 exported JSON restores with new customization defaults", () => {
  const backup = { format: "phios-mobile-backup", schema: 1, data: {
    schema: 1, note: "old export", accent: "amber", compact: false, reducedMotion: true
  }};
  const restored = parseBackup(JSON.stringify(backup));
  assert.equal(restored.note, "old export");
  assert.equal(restored.accent, "amber");
  assert.equal(restored.reducedMotion, true);
  assert.equal(restored.showClock, true);
  assert.equal(restored.showHero, true);
});

test("shortcut order strips duplicates, unknown IDs and fills missing IDs", () => {
  assert.deepEqual(normalizeHomeOrder(["about", "about", "fake", "notes"]), [
    "about", "notes", "vault", "pulse", "settings", "phisurf"
  ]);
  assert.deepEqual(normalizeHomeOrder(null), [...HOME_SHORTCUTS]);
  assert.deepEqual(normalizeHomeOrder("notes"), [...HOME_SHORTCUTS]);
});

test("moving shortcut does not mutate the prior list and clamps boundaries", () => {
  const original = [...HOME_SHORTCUTS];
  const changed = moveShortcut(original, "notes", 2);
  assert.deepEqual(changed.slice(0, 3), ["vault", "pulse", "notes"]);
  assert.deepEqual(original, [...HOME_SHORTCUTS]);
  assert.deepEqual(moveShortcut(original, "notes", -20), original);
  assert.deepEqual(moveShortcut(original, "phisurf", 99), original);
  assert.deepEqual(moveShortcut(original, "unknown", -1), original);
});

test("dial chooses all six wheel sectors and respects center dead zone", () => {
  assert.deepEqual([
    wheelSelection(0, -110), wheelSelection(90, -52), wheelSelection(90, 52),
    wheelSelection(0, 110), wheelSelection(-90, 52), wheelSelection(-90, -52)
  ], [0, 1, 2, 3, 4, 5]);
  assert.equal(wheelSelection(2, 2), -1);
  assert.equal(wheelSelection(NaN, 120), -1);
});

test("home customization survives export and import and rejects invalid values", () => {
  const custom = {
    ...defaultState(), homeOrder: moveShortcut(HOME_SHORTCUTS, "about", -3),
    homeLayout: "list", showClock: false, showHero: false
  };
  const json = serializeBackup(custom);
  assert.match(json, /"schema": 2/);
  assert.deepEqual(parseBackup(json), custom);
  const corrupt = sanitizeState({ ...custom, homeOrder: ["phisurf", "phisurf", "bogus"], homeLayout: "invalid", showClock: "false" });
  assert.equal(corrupt.homeLayout, "grid");
  assert.equal(corrupt.showClock, true);
  assert.deepEqual(corrupt.homeOrder, ["phisurf", "notes", "vault", "pulse", "settings", "about"]);
});
