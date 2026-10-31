import test from "node:test";
import assert from "node:assert/strict";
import {
  STORAGE_KEY, APPS, defaultState, sanitizeState, loadState, persistState,
  serializeBackup, parseBackup, NOTE_LIMIT
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
  assert.throws(() => parseBackup(JSON.stringify({ format: "phios-mobile-backup", schema: 2, data: defaultState() })), /not a supported/);
  assert.throws(() => parseBackup(JSON.stringify({ format: "wrong", schema: 1, data: defaultState() })), /not a supported/);
  assert.throws(() => parseBackup(JSON.stringify({ format: "phios-mobile-backup", schema: 1, data: { note: "x".repeat(NOTE_LIMIT + 1) } })), /exceeds/);
  assert.throws(() => parseBackup("x".repeat(1024 * 1024 + 1)), /under 1 MB/);
  assert.throws(() => parseBackup("{}"), /not a supported/);
});
