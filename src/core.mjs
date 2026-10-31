export const STORAGE_KEY = "phios.mobile.r1";
export const BACKUP_FORMAT = "phios-mobile-backup";
export const SCHEMA_VERSION = 1;
export const NOTE_LIMIT = 50000;
export const ACCENTS = Object.freeze(["cyan", "violet", "amber"]);
export const APPS = Object.freeze([
  { id: "home", name: "Home" },
  { id: "notes", name: "Notes" },
  { id: "vault", name: "Vault" },
  { id: "pulse", name: "Pulse" },
  { id: "settings", name: "Settings" },
  { id: "about", name: "About" }
]);

export function defaultState() {
  return { schema: SCHEMA_VERSION, note: "", accent: "cyan", compact: false, reducedMotion: false };
}

export function sanitizeState(value) {
  const defaults = defaultState();
  if (!value || typeof value !== "object" || Array.isArray(value)) return defaults;
  return {
    schema: SCHEMA_VERSION,
    note: typeof value.note === "string" ? value.note.slice(0, NOTE_LIMIT) : defaults.note,
    accent: ACCENTS.includes(value.accent) ? value.accent : defaults.accent,
    compact: typeof value.compact === "boolean" ? value.compact : defaults.compact,
    reducedMotion: typeof value.reducedMotion === "boolean" ? value.reducedMotion : defaults.reducedMotion
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
  if (!parsed || parsed.format !== BACKUP_FORMAT || parsed.schema !== SCHEMA_VERSION ||
      !parsed.data || typeof parsed.data !== "object" || Array.isArray(parsed.data)) {
    throw new Error("This is not a supported PhiOS Mobile Rung 1 backup.");
  }
  if (typeof parsed.data.note !== "string" || parsed.data.note.length > NOTE_LIMIT) {
    throw new Error("Backup note is missing or exceeds the 50,000-character limit.");
  }
  return sanitizeState(parsed.data);
}
