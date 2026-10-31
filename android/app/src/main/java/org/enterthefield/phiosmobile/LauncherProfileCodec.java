package org.enterthefield.phiosmobile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Small portable JSON contract for local native-launcher configuration.
 * Deliberately excludes account identifiers, phone settings and app data.
 * Import cannot install apps, grant permissions, or choose the default HOME.
 */
public final class LauncherProfileCodec {
    public static final String SCHEMA = "org.enterthefield.phiosmobile.launcher-profile";
    public static final int VERSION = 1;
    public static final int MAX_BYTES = 65536;
    public static final int MAX_PINNED = 256;
    public static final int MAX_KEY_LENGTH = 240;

    public static final String[] LAYOUT_KEYS = {
        "show_clock", "show_hero", "compact_rows", "show_shelf"
    };
    public static final boolean[] LAYOUT_DEFAULTS = {true, true, false, true};

    private LauncherProfileCodec() {}

    public static final class Preview {
        public final Set<String> availablePins;
        public final int skippedUnavailable;
        public final int exportedPins;
        public final Map<String, Boolean> layout;

        private Preview(Set<String> availablePins, int skipped, int total,
                        Map<String, Boolean> layout) {
            this.availablePins = Collections.unmodifiableSet(new LinkedHashSet<>(availablePins));
            this.skippedUnavailable = skipped;
            this.exportedPins = total;
            this.layout = Collections.unmodifiableMap(new LinkedHashMap<>(layout));
        }
    }

    private static boolean validComponentKey(String key) {
        if (key == null || key.isEmpty() || key.length() > MAX_KEY_LENGTH) return false;
        return key.matches("[A-Za-z_][A-Za-z_0-9]*(?:\\.[A-Za-z_][A-Za-z_0-9]*)+/[A-Za-z_.$][A-Za-z_0-9.$]*");
    }

    private static void validateLayout(Map<String, Boolean> layout) {
        if (layout == null || layout.size() != LAYOUT_KEYS.length)
            throw new IllegalArgumentException("Launcher layout has missing or unexpected settings.");
        for (String key : LAYOUT_KEYS) {
            if (!layout.containsKey(key) || layout.get(key) == null)
                throw new IllegalArgumentException("Missing boolean layout setting: " + key);
        }
    }

    public static String encode(Set<String> pinned, Map<String, Boolean> layout)
            throws JSONException {
        validateLayout(layout);
        if (pinned == null || pinned.size() > MAX_PINNED)
            throw new IllegalArgumentException("Too many pinned apps to export.");
        List<String> keys = new ArrayList<>(pinned);
        Collections.sort(keys);
        JSONArray array = new JSONArray();
        for (String key : keys) {
            if (!validComponentKey(key))
                throw new IllegalArgumentException("Invalid pinned app identifier.");
            array.put(key);
        }
        JSONObject options = new JSONObject();
        for (String key : LAYOUT_KEYS) options.put(key, layout.get(key));
        JSONObject root = new JSONObject();
        root.put("schema", SCHEMA);
        root.put("version", VERSION);
        root.put("pinned", array);
        root.put("layout", options);
        String output = root.toString(2) + "\n";
        if (output.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw new IllegalArgumentException("Launcher profile exceeds 64 KiB.");
        return output;
    }

    public static Preview decode(byte[] input, Set<String> installed)
            throws JSONException, CharacterCodingException {
        if (input == null || input.length == 0 || input.length > MAX_BYTES)
            throw new IllegalArgumentException("Profile must be between 1 byte and 64 KiB.");
        if (installed == null) throw new IllegalArgumentException("Installed apps unavailable.");

        // Do not silently turn invalid UTF-8 into replacement characters.
        String json = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(input)).toString();
        JSONObject root = new JSONObject(json);
        if (root.length() != 4 || !(root.opt("schema") instanceof String)
                || !SCHEMA.equals(root.getString("schema")))
            throw new IllegalArgumentException("Unknown launcher profile schema.");
        Object version = root.opt("version");
        if (!(version instanceof Integer) && !(version instanceof Long))
            throw new IllegalArgumentException("Launcher profile version must be an integer.");
        if (((Number) version).longValue() != VERSION)
            throw new IllegalArgumentException("Unsupported launcher profile version.");
        Object rawPins = root.opt("pinned");
        Object rawLayout = root.opt("layout");
        if (!(rawPins instanceof JSONArray) || !(rawLayout instanceof JSONObject))
            throw new IllegalArgumentException("Missing pinned apps or layout object.");

        JSONArray imported = (JSONArray) rawPins;
        if (imported.length() > MAX_PINNED)
            throw new IllegalArgumentException("Too many pinned apps.");
        Set<String> valid = new LinkedHashSet<>();
        Set<String> seen = new HashSet<>();
        int skipped = 0;
        for (int i = 0; i < imported.length(); i++) {
            Object raw = imported.get(i);
            if (!(raw instanceof String) || !validComponentKey((String) raw))
                throw new IllegalArgumentException("Invalid pinned app identifier at index " + i);
            String key = (String) raw;
            if (!seen.add(key)) throw new IllegalArgumentException("Duplicate pinned app identifier.");
            if (installed.contains(key)) valid.add(key);
            else skipped++;
        }

        JSONObject options = (JSONObject) rawLayout;
        if (options.length() != LAYOUT_KEYS.length)
            throw new IllegalArgumentException("Unexpected or missing layout setting.");
        Map<String, Boolean> layout = new LinkedHashMap<>();
        for (String key : LAYOUT_KEYS) {
            Object value = options.opt(key);
            if (!(value instanceof Boolean))
                throw new IllegalArgumentException("Layout setting must be boolean: " + key);
            layout.put(key, (Boolean) value);
        }
        return new Preview(valid, skipped, imported.length(), layout);
    }
}
