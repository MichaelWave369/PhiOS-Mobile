/* Field Companion v1, independent from PhiOperator and Android HOME. */
import {createFieldCompanionReceiver, parseFieldCompanion, FIELD_ROUTES, FIELD_MAX_BYTES} from './field-companion.mjs';

const NAMES = {live:'PhiOS Live',xr:'PhiOS XR',mobile:'PhiOS Mobile'};
function byId(id) {return document.getElementById(id);}
export function initFieldReceiver({showToast = () => {}} = {}) {
  const fileInput = byId('field-receiver-import');
  const indicator = byId('field-receiver-status');
  const label = byId('field-receiver-label');
  const routes = byId('field-receiver-routes');
  const exportButton = byId('field-receiver-export');
  const removeButton = byId('field-receiver-remove');
  if (!fileInput || !indicator || !label || !routes || !exportButton || !removeButton) return null;
  let storage = null;
  try {storage = window.localStorage;} catch { /* optional */ }
  const receiver = createFieldCompanionReceiver({storage});
  const refresh = () => {
    const packet = receiver.snapshot();
    label.textContent = packet
      ? (packet.profile.alias || 'Field Explorer') + ' · DISPLAY ONLY'
      : 'No Field Companion preferences imported. All features remain available.';
    exportButton.disabled = !packet;
    removeButton.disabled = !packet;
    routes.replaceChildren();
    if (!packet) return;
    const ids = [...packet.profile.pinned];
    if (packet.profile.last && !ids.includes(packet.profile.last)) ids.push(packet.profile.last);
    if (!ids.length) {
      const text = document.createElement('span');
      text.textContent = 'No pinned destinations or last route.';
      routes.append(text);
      return;
    }
    for (const id of ids) {
      const anchor = document.createElement('a');
      anchor.href = FIELD_ROUTES[id];
      anchor.className = 'external-link';
      anchor.textContent = NAMES[id] + ' ↗';
      routes.append(anchor);
    }
  };
  fileInput.addEventListener('change', async () => {
    const file = fileInput.files?.[0];
    fileInput.value = '';
    if (!file) return;
    try {
      if (file.size > FIELD_MAX_BYTES) throw new TypeError('File is too large (16 KiB max).');
      const text = await file.text();
      parseFieldCompanion(text); // validate before prompting or mutating state
      if (!window.confirm(
        'Import this Field Companion callsign and portal preferences into this browser only? No app settings, Android Home, Passport, or permissions change.'
      )) {
        indicator.textContent = 'Import canceled. No changes made.';
        return;
      }
      const outcome = receiver.importJSON(text);
      refresh();
      indicator.textContent = outcome.persisted
        ? 'Companion preferences stored in this browser.'
        : 'Session-only import: browser storage unavailable. Export a copy.';
      showToast('Field Companion preferences imported.');
    } catch {
      indicator.textContent = 'Import rejected: invalid or unsupported Field Companion v1 JSON.';
    }
  });
  exportButton.addEventListener('click', () => {
    const contents = receiver.exportJSON();
    if (!contents) return;
    const url = URL.createObjectURL(new Blob([contents], {type:'application/json'}));
    const a = document.createElement('a');
    a.href = url; a.download = 'phi-field-companion-v1.json';
    document.body.appendChild(a); a.click(); a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1500);
    indicator.textContent = 'Plaintext JSON exported. Keep it somewhere you control.';
  });
  removeButton.addEventListener('click', () => {
    if (!receiver.snapshot() || !window.confirm(
      'Remove only imported Field Companion preferences? Notes, Vault, Operator Passport, and Android Home remain untouched.'
    )) return;
    receiver.clear();
    refresh();
    indicator.textContent = 'Imported Field Companion preferences removed.';
  });
  refresh();
  return receiver;
}
