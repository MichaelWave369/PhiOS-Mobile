export const STORAGE_KEY = "phios.mobile.r1";
export const BACKUP_FORMAT = "phios-mobile-backup";
export const SCHEMA_VERSION = 2;
export const NOTE_LIMIT = 50000;
export const ACCENTS = Object.freeze(["cyan", "violet", "amber"]);
export const HOME_SHORTCUTS = Object.freeze(["notes", "vault", "pulse", "settings", "about", "phisurf"]);
export const HOME_LAYOUTS = Object.freeze(["grid", "list"]);
export const APPS = Object.freeze([
  { id: "home", name: "Home" },
  { id: "notes", name: "Notes" },
  { id: "vault", name: "Vault" },
  { id: "pulse", name: "Pulse" },
  { id: "settings", name: "Settings" },
  { id: "about", name: "About" }
]);

export function defaultState() {
  return {
    schema: SCHEMA_VERSION, note: "", accent: "cyan", compact: false,
    reducedMotion: false, homeOrder: [...HOME_SHORTCUTS],
    homeLayout: "grid", showClock: true, showHero: true
  };
}

export function normalizeHomeOrder(input) {
  if (!Array.isArray(input)) return [...HOME_SHORTCUTS];
  const unique = [...new Set(input.filter(id => typeof id === "string" && HOME_SHORTCUTS.includes(id)))];
  return [...unique, ...HOME_SHORTCUTS.filter(id => !unique.includes(id))];
}

export function moveShortcut(order, id, offset) {
  const normalized = normalizeHomeOrder(order);
  const position = normalized.indexOf(id);
  if (position < 0 || !Number.isInteger(offset)) return normalized;
  const target = Math.max(0, Math.min(normalized.length - 1, position + offset));
  const result = [...normalized];
  result.splice(position, 1);
  result.splice(target, 0, id);
  return result;
}

// The angle is measured clockwise from the top of the wheel.
// Non-finite input intentionally selects nothing.
export function wheelSelection(dx, dy, deadZone = 42) {
  if (![dx, dy, deadZone].every(Number.isFinite) || Math.hypot(dx, dy) < deadZone) return -1;
  const angle = Math.atan2(dy, dx);
  return ((Math.round((angle + Math.PI / 2) / (Math.PI / 3)) % 6) + 6) % 6;
}

export function sanitizeState(value) {
  const defaults = defaultState();
  if (!value || typeof value !== "object" || Array.isArray(value)) return defaults;
  return {
    schema: SCHEMA_VERSION,
    note: typeof value.note === "string" ? value.note.slice(0, NOTE_LIMIT) : defaults.note,
    accent: ACCENTS.includes(value.accent) ? value.accent : defaults.accent,
    compact: typeof value.compact === "boolean" ? value.compact : defaults.compact,
    reducedMotion: typeof value.reducedMotion === "boolean" ? value.reducedMotion : defaults.reducedMotion,
    homeOrder: normalizeHomeOrder(value.homeOrder),
    homeLayout: HOME_LAYOUTS.includes(value.homeLayout) ? value.homeLayout : defaults.homeLayout,
    showClock: typeof value.showClock === "boolean" ? value.showClock : defaults.showClock,
    showHero: typeof value.showHero === "boolean" ? value.showHero : defaults.showHero
  };
}

export function loadState(storage) {
  try {
    if (!storage) return defaultState();
    const raw = storage.getItem(STORAGE_KEY);
    return raw === null ? defaultState() : sanitizeState(JSON.parse(raw));
  } catch {
    return defaultState();
  }
}

export function persistState(storage, state) {
  try {
    if (!storage) return false;
    storage.setItem(STORAGE_KEY, JSON.stringify(sanitizeState(state)));
    return true;
  } catch {
    return false;
  }
}

export function serializeBackup(state, date = new Date()) {
  return JSON.stringify({
    format: BACKUP_FORMAT,
    schema: SCHEMA_VERSION,
    exportedAt: date.toISOString(),
    data: sanitizeState(state)
  }, null, 2);
}

export function parseBackup(text) {
  if (typeof text !== "string" || text.length > 1024 * 1024) {
    throw new Error("Backup must be a JSON file under 1 MB.");
  }
  let parsed;
  try { parsed = JSON.parse(text); }
  catch { throw new Error("This file is not valid JSON."); }
  if (!parsed || parsed.format !== BACKUP_FORMAT || ![1, SCHEMA_VERSION].includes(parsed.schema) ||
      !parsed.data || typeof parsed.data !== "object" || Array.isArray(parsed.data)) {
    throw new Error("This is not a supported PhiOS Mobile backup.");
  }
  if (typeof parsed.data.note !== "string" || parsed.data.note.length > NOTE_LIMIT) {
    throw new Error("Backup note is missing or exceeds the 50,000-character limit.");
  }
  // Rung 1 files (schema 1) keep their note and settings.
  // Missing Rung 1.1 fields receive defaults, not guessed permissions.
  return sanitizeState(parsed.data);
}
