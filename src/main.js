import {
  STORAGE_KEY, NOTE_LIMIT, APPS, defaultState, loadState, persistState,
  serializeBackup, parseBackup
} from "./core.mjs";

const byId = (id) => document.getElementById(id);
const storage = (() => {
  try { return window.localStorage; } catch { return null; }
})();
let state = loadState(storage);
let activeScreen = "home";
let previousFocus = null;
let toastTimer;

const surf = byId("phisurf");
const screens = [...document.querySelectorAll("[data-screen]")];
const surfBackground = [document.querySelector(".statusbar"), document.querySelector(".topbar"), byId("screens"), document.querySelector(".dock")];

function showToast(message) {
  const node = byId("toast");
  node.textContent = message;
  node.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { node.hidden = true; }, 3600);
}

function paintPreferences() {
  document.documentElement.dataset.accent = state.accent;
  document.documentElement.classList.toggle("compact", state.compact);
  document.documentElement.classList.toggle("reduce-motion", state.reducedMotion);
  for (const radio of document.querySelectorAll('input[name="accent"]')) {
    radio.checked = radio.value === state.accent;
  }
  byId("compact-toggle").checked = state.compact;
  byId("motion-toggle").checked = state.reducedMotion;
}

function updateNote() {
  byId("note-input").value = state.note;
  byId("note-count").textContent = state.note.length.toLocaleString() + " / " + NOTE_LIMIT.toLocaleString();
}

function save() {
  const success = persistState(storage, state);
  byId("save-status").textContent = success ? "Saved locally in this browser." : "Not saved: browser storage is unavailable.";
  return success;
}

function openSurf() {
  if (!surf.hidden) return;
  previousFocus = document.activeElement;
  surf.hidden = false;
  for (const node of surfBackground) node.inert = true;
  byId("close-surf").focus();
}

function closeSurf(restoreFocus = true) {
  if (surf.hidden) return;
  surf.hidden = true;
  for (const node of surfBackground) node.inert = false;
  if (restoreFocus && previousFocus && typeof previousFocus.focus === "function") previousFocus.focus();
  previousFocus = null;
}

function openScreen(screenName) {
  if (!APPS.some((app) => app.id === screenName)) return;
  activeScreen = screenName;
  closeSurf(false);
  for (const node of screens) node.hidden = node.dataset.screen !== screenName;
  for (const button of document.querySelectorAll(".dock [data-open]")) {
    if (button.dataset.open === screenName) button.setAttribute("aria-current", "page");
    else button.removeAttribute("aria-current");
  }
  byId("screens").scrollTop = 0;
  if (screenName === "pulse") updatePulse();
  const heading = byId("screen-" + screenName).querySelector("h1");
  if (heading) { heading.tabIndex = -1; heading.focus({ preventScroll: true }); }
}

function updateClock() {
  const now = new Date();
  const time = new Intl.DateTimeFormat(undefined, { hour: "numeric", minute: "2-digit" }).format(now);
  byId("status-time").textContent = time;
  byId("home-time").textContent = time;
  byId("home-date").textContent = new Intl.DateTimeFormat(undefined, {
    weekday: "long", month: "long", day: "numeric"
  }).format(now);
}

function canWriteStorage() {
  if (!storage) return false;
  const key = STORAGE_KEY + ".probe";
  try { storage.setItem(key, "1"); storage.removeItem(key); return true; }
  catch { return false; }
}

function updatePulse() {
  const online = navigator.onLine;
  byId("status-network").textContent = online ? "NET HINT +" : "OFFLINE HINT";
  byId("pulse-network").textContent = online ? "Possibly online" : "Reported offline";
  byId("pulse-secure").textContent = window.isSecureContext ? "Yes" : "No";
  byId("pulse-worker").textContent = "serviceWorker" in navigator
    ? (navigator.serviceWorker.controller ? "Controlling app" : "Not controlling")
    : "Unsupported";
  byId("pulse-install").textContent = window.matchMedia("(display-mode: standalone)").matches || navigator.standalone
    ? "Standalone" : "Browser tab";
  byId("pulse-storage").textContent = canWriteStorage() ? "Available" : "Unavailable";
}

for (const button of document.querySelectorAll("[data-open]")) {
  button.addEventListener("click", () => openScreen(button.dataset.open));
}
for (const id of ["open-surf-top", "open-surf-dock", "grid-surf", "home-surf"]) {
  byId(id).addEventListener("click", openSurf);
}
byId("close-surf").addEventListener("click", () => closeSurf());
surf.addEventListener("click", (event) => { if (event.target === surf) closeSurf(); });

document.addEventListener("keydown", (event) => {
  if (event.altKey && event.key.toLowerCase() === "s") {
    event.preventDefault();
    if (surf.hidden) openSurf(); else closeSurf();
  }
  if (event.key === "Escape" && !surf.hidden) { event.preventDefault(); closeSurf(); }
  if (event.key === "Tab" && !surf.hidden) {
    const buttons = [...surf.querySelectorAll("button:not([disabled])")];
    const first = buttons[0], last = buttons[buttons.length - 1];
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
    if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  }
});

let startPointer = null;
document.addEventListener("pointerdown", (event) => {
  startPointer = surf.hidden && event.clientX <= 26 ? { x: event.clientX, y: event.clientY } : null;
}, { passive: true });
document.addEventListener("pointerup", (event) => {
  if (!startPointer) return;
  const dx = event.clientX - startPointer.x;
  const dy = Math.abs(event.clientY - startPointer.y);
  startPointer = null;
  if (dx > 65 && dy < 80) openSurf();
}, { passive: true });
document.addEventListener("pointercancel", () => { startPointer = null; });

byId("note-input").addEventListener("input", (event) => {
  state.note = event.target.value.slice(0, NOTE_LIMIT);
  byId("note-count").textContent = state.note.length.toLocaleString() + " / " + NOTE_LIMIT.toLocaleString();
  save();
});
byId("clear-note").addEventListener("click", () => {
  if (!window.confirm("Clear your locally stored note?")) return;
  state.note = "";
  updateNote();
  save();
  showToast("Note cleared.");
});

for (const radio of document.querySelectorAll('input[name="accent"]')) {
  radio.addEventListener("change", () => {
    if (!radio.checked) return;
    state.accent = radio.value;
    paintPreferences();
    save();
  });
}
byId("compact-toggle").addEventListener("change", (event) => {
  state.compact = event.target.checked;
  paintPreferences();
  save();
});
byId("motion-toggle").addEventListener("change", (event) => {
  state.reducedMotion = event.target.checked;
  paintPreferences();
  save();
});

byId("export-button").addEventListener("click", () => {
  const json = serializeBackup(state);
  const blob = new Blob([json], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "phios-mobile-backup.json";
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
  showToast("Backup downloaded as plaintext JSON.");
});

byId("restore-file").addEventListener("change", async (event) => {
  const input = event.target;
  const file = input.files && input.files[0];
  if (!file) return;
  try {
    if (file.size > 1024 * 1024) throw new Error("Backup must be smaller than 1 MB.");
    const candidate = parseBackup(await file.text());
    if (!window.confirm("Replace your existing local note and preferences with this backup?")) return;
    state = candidate;
    const success = save();
    paintPreferences();
    updateNote();
    showToast(success ? "Backup restored locally." : "Loaded for this session, but storage is unavailable.");
  } catch (error) {
    showToast(error.message || "Could not import backup.");
  } finally {
    input.value = "";
  }
});

byId("erase-button").addEventListener("click", () => {
  if (!window.confirm("Erase this prototype's stored note and settings? This cannot be undone without a backup.")) return;
  let success = false;
  try { if (storage) { storage.removeItem(STORAGE_KEY); success = true; } } catch { /* storage blocked */ }
  state = defaultState();
  paintPreferences();
  updateNote();
  byId("save-status").textContent = success ? "No saved local note." : "Storage is unavailable.";
  showToast(success ? "Local PhiOS Mobile data erased." : "Local storage could not be cleared.");
});

byId("refresh-pulse").addEventListener("click", updatePulse);
window.addEventListener("online", updatePulse);
window.addEventListener("offline", updatePulse);

async function registerOfflineShell() {
  if (!("serviceWorker" in navigator) || !window.isSecureContext) return;
  try {
    await navigator.serviceWorker.register("./sw.js", { scope: "./" });
    navigator.serviceWorker.addEventListener("controllerchange", updatePulse);
  } catch (error) {
    console.warn("Offline worker not installed:", error);
  } finally { updatePulse(); }
}

paintPreferences();
updateNote();
updateClock();
updatePulse();
openScreen(activeScreen);
setInterval(updateClock, 30000);
void registerOfflineShell();
