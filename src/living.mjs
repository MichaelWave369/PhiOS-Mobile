import { HOME_SHORTCUTS, moveShortcut, normalizeHomeOrder, wheelSelection } from "./core.mjs";

const labels = {
  notes: "Notes", vault: "Vault", pulse: "Pulse",
  settings: "Settings", about: "About", phisurf: "PhiSurf"
};

export function initLivingShell({ getState, applyState, openScreen, showToast, openSurf }) {
  const byId = (id) => document.getElementById(id);
  const quick = byId("quick-panel");
  const quickButton = byId("open-quick");
  const grid = byId("app-grid");
  const editor = byId("home-editor");
  const editorList = byId("home-shortcuts-list");
  const surf = byId("phisurf");
  const wheel = surf.querySelector(".surf-wheel");
  const wheelButtons = [...wheel.querySelectorAll(".wheel-app")];
  const backdrop = [
    document.querySelector(".statusbar"), document.querySelector(".topbar"),
    byId("screens"), document.querySelector(".dock")
  ];
  let quickFocus = null;
  let gesture = null;
  let wheelPointer = null;
  let selected = -1;

  function sync() {
    const state = getState();
    const order = normalizeHomeOrder(state.homeOrder);
    for (const id of order) {
      const tile = grid.querySelector('[data-shortcut="' + id + '"]');
      if (tile) grid.appendChild(tile);
    }
    grid.classList.toggle("layout-list", state.homeLayout === "list");
    byId("clock-block").hidden = !state.showClock;
    byId("hero-card").hidden = !state.showHero;
    for (const radio of document.querySelectorAll('input[name="home-layout"]')) {
      radio.checked = radio.value === state.homeLayout;
    }
    byId("clock-toggle").checked = state.showClock;
    byId("hero-toggle").checked = state.showHero;
    byId("quick-compact").checked = state.compact;
    byId("quick-motion").checked = state.reducedMotion;
    byId("quick-accent-label").textContent = state.accent;
    renderEditor(order);
    updateQuickHint();
  }

  function renderEditor(order) {
    editorList.replaceChildren();
    for (let i = 0; i < order.length; i++) {
      const id = order[i];
      const row = document.createElement("li");
      row.className = "edit-shortcut";
      const title = document.createElement("span");
      title.textContent = (i + 1) + ". " + labels[id];
      row.appendChild(title);
      for (const [offset, symbol, direction] of [[-1, "↑", "earlier"], [1, "↓", "later"]]) {
        const button = document.createElement("button");
        button.type = "button";
        button.dataset.move = String(offset);
        button.dataset.shortcut = id;
        button.textContent = symbol;
        button.setAttribute("aria-label", "Move " + labels[id] + " " + direction);
        button.disabled = offset === -1 ? i === 0 : i === order.length - 1;
        row.appendChild(button);
      }
      editorList.appendChild(row);
    }
  }

  function updateQuickHint() {
    byId("quick-network").textContent = navigator.onLine
      ? "Browser reports possibly online"
      : "Browser reports offline";
  }

  function openQuick() {
    if (!quick.hidden || !surf.hidden) return;
    quickFocus = document.activeElement;
    quick.hidden = false;
    for (const element of backdrop) element.inert = true;
    byId("close-quick").focus();
    updateQuickHint();
  }

  function closeQuick(restoreFocus = true) {
    if (quick.hidden) return;
    quick.hidden = true;
    for (const element of backdrop) element.inert = false;
    if (restoreFocus && quickFocus?.isConnected) quickFocus.focus();
    quickFocus = null;
  }

  quickButton.addEventListener("click", openQuick);
  byId("close-quick").addEventListener("click", () => closeQuick());
  byId("quick-note").addEventListener("click", () => { closeQuick(false); openScreen("notes"); });
  byId("quick-vault").addEventListener("click", () => { closeQuick(false); openScreen("vault"); });
  byId("quick-compact").addEventListener("change", event => {
    applyState({ compact: event.target.checked });
  });
  byId("quick-motion").addEventListener("change", event => {
    applyState({ reducedMotion: event.target.checked });
  });
  byId("quick-accent").addEventListener("click", () => {
    const accents = ["cyan", "violet", "amber"];
    const next = accents[(accents.indexOf(getState().accent) + 1) % accents.length];
    applyState({ accent: next });
  });
  byId("customize-home").addEventListener("click", () => {
    editor.hidden = !editor.hidden;
    byId("customize-home").setAttribute("aria-expanded", String(!editor.hidden));
    if (!editor.hidden) editor.scrollIntoView({ block: "nearest", behavior: "auto" });
  });
  byId("close-home-editor").addEventListener("click", () => {
    editor.hidden = true;
    byId("customize-home").setAttribute("aria-expanded", "false");
    byId("customize-home").focus();
  });
  byId("reset-home").addEventListener("click", () => {
    applyState({ homeOrder: [...HOME_SHORTCUTS], homeLayout: "grid", showClock: true, showHero: true });
    showToast("Home layout restored to default.");
  });
  editorList.addEventListener("click", event => {
    const button = event.target.closest("button[data-move]");
    if (!button || !editorList.contains(button)) return;
    const { shortcut, move } = button.dataset;
    applyState({ homeOrder: moveShortcut(getState().homeOrder, shortcut, Number(move)) });
    const replacement = editorList.querySelector('button[data-shortcut="' + shortcut + '"][data-move="' + move + '"]');
    if (replacement && !replacement.disabled) replacement.focus();
    else editorList.querySelector('button[data-shortcut="' + shortcut + '"]:not(:disabled)')?.focus();
  });
  for (const radio of document.querySelectorAll('input[name="home-layout"]')) {
    radio.addEventListener("change", () => { if (radio.checked) applyState({ homeLayout: radio.value }); });
  }
  for (const [inputId, key] of [["clock-toggle", "showClock"], ["hero-toggle", "showHero"]]) {
    byId(inputId).addEventListener("change", event => applyState({ [key]: event.target.checked }));
  }
  window.addEventListener("online", updateQuickHint);
  window.addEventListener("offline", updateQuickHint);

  function paintWheel(index) {
    if (index === selected) return;
    selected = index;
    for (const [i, button] of wheelButtons.entries()) {
      const active = i === index;
      button.classList.toggle("dial-active", active);
      button.setAttribute("aria-pressed", String(active));
    }
    const label = byId("dial-selection");
    label.textContent = index >= 0 ? labels[wheelButtons[index].dataset.open] + " · release to open" : "Drag outward to choose";
  }

  wheel.addEventListener("pointerdown", event => {
    if (!surf.hidden && !event.target.closest("button")) {
      wheelPointer = { id: event.pointerId };
      wheel.setPointerCapture(event.pointerId);
      paintWheel(-1);
    }
  });
  wheel.addEventListener("pointermove", event => {
    if (!wheelPointer || wheelPointer.id !== event.pointerId) return;
    const bounds = wheel.getBoundingClientRect();
    const dx = event.clientX - (bounds.left + bounds.width / 2);
    const dy = event.clientY - (bounds.top + bounds.height / 2);
    paintWheel(wheelSelection(dx, dy, bounds.width < 290 ? 30 : 42));
  });
  wheel.addEventListener("pointerup", event => {
    if (!wheelPointer || wheelPointer.id !== event.pointerId) return;
    const chosen = selected;
    wheelPointer = null;
    paintWheel(-1);
    if (chosen >= 0) openScreen(wheelButtons[chosen].dataset.open);
  });
  wheel.addEventListener("pointercancel", () => { wheelPointer = null; paintWheel(-1); });
  surf.addEventListener("keydown", event => {
    if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") return;
    event.preventDefault();
    const focused = wheelButtons.indexOf(document.activeElement);
    const current = focused < 0 ? (selected < 0 ? 0 : selected) : focused;
    const step = event.key === "ArrowRight" ? 1 : -1;
    const next = (current + step + wheelButtons.length) % wheelButtons.length;
    paintWheel(next);
    wheelButtons[next].focus();
  });
  surf.addEventListener("click", event => {
    if (event.target.closest(".wheel-app")) paintWheel(-1);
  });

  document.addEventListener("keydown", event => {
    if (quick.hidden) return;
    if (event.key === "Escape") {
      event.preventDefault();
      closeQuick();
    }
    if (event.key !== "Tab") return;
    const controls = [...quick.querySelectorAll("button:not([disabled]), input:not([disabled])")];
    const first = controls[0], last = controls[controls.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault(); last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault(); first.focus();
    }
  });

  // Gesture lanes are intentionally edge-bound to avoid hijacking normal scrolling.
  // Top edge downward: quick panel. Right edge inward: home. Left edge: PhiSurf (Rung 1).
  const device = byId("device");
  device.addEventListener("pointerdown", event => {
    if (!surf.hidden || !quick.hidden || event.target.closest("button,input,textarea,a,label")) return;
    const rect = device.getBoundingClientRect();
    const x = event.clientX - rect.left, y = event.clientY - rect.top;
    if (y <= 55) gesture = { id: event.pointerId, kind: "quick", x, y };
    else if (x >= rect.width - 24) gesture = { id: event.pointerId, kind: "home", x, y };
  }, { passive: true });
  device.addEventListener("pointerup", event => {
    if (!gesture || gesture.id !== event.pointerId) return;
    const rect = device.getBoundingClientRect();
    const dx = event.clientX - rect.left - gesture.x;
    const dy = event.clientY - rect.top - gesture.y;
    if (gesture.kind === "quick" && dy > 75 && Math.abs(dx) < 65) openQuick();
    if (gesture.kind === "home" && dx < -75 && Math.abs(dy) < 65) openScreen("home");
    gesture = null;
  }, { passive: true });
  device.addEventListener("pointercancel", () => { gesture = null; });

  sync();
  return { sync, closeQuick, openQuick, clearDial: () => paintWheel(-1) };
}
