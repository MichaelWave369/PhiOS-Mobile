package org.enterthefield.phiosmobile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Offline public SOURCE bookmark interchange, identical to PhiOS Web R2.
 * It cannot install APKs, alter Android HOME, or grant execution authority.
 */
public final class AppShelfCodec {
    public static final String FORMAT = "phios-app-shelf";
    public static final int VERSION = 1;
    public static final int MAX_BYTES = 16384;
    public static final int MAX_PINS = 24;
    public static final int MAX_CATALOG_BYTES = 524288;
    public static final String SOURCE_SHA = "e295f6265e60032de98a2acd8e53295505fc47e8";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter
            .ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
    private static final Set<String> WIRE_KEYS = Set.of(
            "format", "version", "catalog_source_sha", "exported_at", "pinned");
    private AppShelfCodec() {}

    public static final class Source {
        public final String id;
        public final String name;
        public final String repository;
        public final String description;
        private Source(String id, String name, String repository, String description) {
            this.id = id;
            this.name = name;
            this.repository = repository;
            this.description = description;
        }
    }

    public static final class Catalog {
        public final Map<String, Source> sources;
        public final String sha;

        private Catalog(Map<String, Source> sources, String sha) {
            this.sources = Collections.unmodifiableMap(new LinkedHashMap<>(sources));
            this.sha = sha;
        }
    }

    public static Catalog readCatalog(byte[] bytes) throws JSONException, CharacterCodingException {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_CATALOG_BYTES)
            throw new IllegalArgumentException("Public catalog absent or oversized.");
        JSONObject root = new JSONObject(decodeUtf8(bytes));
        if (!"phios.public-repo-catalog.v1".equals(root.optString("schema", "")))
            throw new IllegalArgumentException("Unknown catalog schema.");
        JSONObject proof = root.getJSONObject("provenance");
        JSONObject policy = root.getJSONObject("policy");
        if (!"MichaelWave369".equals(proof.optString("owner"))
                || !"public".equals(proof.optString("scope"))
                || !Boolean.TRUE.equals(proof.opt("complete"))
                || !SOURCE_SHA.equals(proof.optString("source_blob_sha"))
                || !Boolean.TRUE.equals(policy.opt("review_required"))
                || !Boolean.FALSE.equals(policy.opt("automatic_install"))
                || !Boolean.FALSE.equals(policy.opt("automatic_launch"))
                || !Boolean.FALSE.equals(policy.opt("cross_platform_qualification")))
            throw new IllegalArgumentException("Public catalog source or authority mismatch.");
        JSONArray entries = root.getJSONArray("apps");
        if (entries.length() != 225)
            throw new IllegalArgumentException("Expected pinned 225-source snapshot.");
        Map<String, Source> sources = new LinkedHashMap<>();
        Set<String> names = new HashSet<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject raw = entries.getJSONObject(i);
            String id = raw.getString("id");
            String name = raw.getString("name");
            String repository = raw.getString("repository");
            String description = raw.getString("description");
            if (!id.matches("github:[0-9]{1,20}")
                    || !name.matches("[A-Za-z0-9_.-]{1,100}")
                    || !repository.equals("https://github.com/MichaelWave369/" + name)
                    || !"source_only".equals(raw.optString("availability"))
                    || description.length() > 600
                    || sources.containsKey(id) || !names.add(name.toLowerCase(java.util.Locale.ROOT)))
                throw new IllegalArgumentException("Untrusted or duplicate source record.");
            sources.put(id, new Source(id, name, repository, description));
        }
        return new Catalog(sources, SOURCE_SHA);
    }

    private static String decodeUtf8(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
    }

    public static List<String> validate(List<String> pins, Catalog catalog) {
        if (catalog == null || pins == null || pins.size() > MAX_PINS)
            throw new IllegalArgumentException("Shelf is missing or exceeds 24 pins.");
        List<String> clean = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String pin : pins) {
            if (pin == null || !pin.matches("github:[0-9]{1,20}")
                    || !catalog.sources.containsKey(pin) || !seen.add(pin))
                throw new IllegalArgumentException("Unknown or duplicate public source ID.");
            clean.add(pin);
        }
        return clean;
    }

    public static String encode(List<String> pins, Catalog catalog, Instant at) throws JSONException {
        List<String> clean = validate(pins, catalog);
        if (at == null) throw new IllegalArgumentException("Timestamp required.");
        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("version", VERSION);
        root.put("catalog_source_sha", catalog.sha);
        root.put("exported_at", CLOCK.format(at));
        root.put("pinned", new JSONArray(clean));
        String result = root.toString(2) + "\n";
        if (result.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw new IllegalArgumentException("Shelf backup exceeds 16 KiB.");
        return result;
    }

    public static List<String> decode(byte[] bytes, Catalog catalog)
            throws CharacterCodingException, JSONException {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES)
            throw new IllegalArgumentException("Shelf backup must fit 16 KiB.");
        JSONObject root = new JSONObject(decodeUtf8(bytes));
        if (root.length() != WIRE_KEYS.size())
            throw new IllegalArgumentException("Unsupported shelf backup fields.");
        for (String key : WIRE_KEYS) {
            if (!root.has(key)) throw new IllegalArgumentException("Missing shelf field.");
        }
        if (!FORMAT.equals(root.opt("format"))
                || !(root.opt("version") instanceof Number)
                || !Integer.valueOf(VERSION).equals(root.opt("version"))
                || !catalog.sha.equals(root.opt("catalog_source_sha")))
            throw new IllegalArgumentException("Unsupported shelf version or stale catalog.");
        String timestamp = root.optString("exported_at", "");
        if (!timestamp.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}Z"))
            throw new IllegalArgumentException("Invalid shelf timestamp.");
        try { Instant.parse(timestamp); }
        catch (RuntimeException error) { throw new IllegalArgumentException("Invalid shelf timestamp.", error); }
        JSONArray raw = root.getJSONArray("pinned");
        if (raw.length() > MAX_PINS) throw new IllegalArgumentException("Too many shelf pins.");
        List<String> pins = new ArrayList<>();
        for (int i = 0; i < raw.length(); i++) {
            Object value = raw.get(i);
            if (!(value instanceof String)) throw new IllegalArgumentException("Shelf pin must be a string.");
            pins.add((String) value);
        }
        return validate(pins, catalog);
    }
}
