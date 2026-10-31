package org.enterthefield.phiosmobile;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class LauncherProfileCodecTest {
    private final String a = "org.example.first/.Main";
    private final String b = "org.example.second/org.example.second.Home";

    private Map<String, Boolean> layout() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (int i = 0; i < LauncherProfileCodec.LAYOUT_KEYS.length; i++) {
            result.put(LauncherProfileCodec.LAYOUT_KEYS[i],
                       LauncherProfileCodec.LAYOUT_DEFAULTS[i]);
        }
        return result;
    }

    private byte[] bytes(String content) { return content.getBytes(StandardCharsets.UTF_8); }

    @Test public void roundTripAndSkipUninstalledApps() throws Exception {
        String encoded = LauncherProfileCodec.encode(new HashSet<>(Arrays.asList(a, b)), layout());
        LauncherProfileCodec.Preview p =
                LauncherProfileCodec.decode(bytes(encoded), Collections.singleton(a));
        assertEquals(2, p.exportedPins);
        assertEquals(1, p.skippedUnavailable);
        assertEquals(Collections.singleton(a), p.availablePins);
        assertEquals(layout(), p.layout);
        assertFalse(encoded.contains("org.example.someSecret"));
    }

    @Test public void rejectsUnknownSchemaAndVersion() throws Exception {
        String good = LauncherProfileCodec.encode(Collections.singleton(a), layout());
        rejects(good.replace(LauncherProfileCodec.SCHEMA, "another-app-schema"));
        rejects(good.replace("\"version\": 1", "\"version\": 42"));
        rejects(good.replace("\"version\": 1", "\"version\": \"1\""));
    }

    @Test public void rejectsUnknownLayoutFieldsAndWrongTypes() throws Exception {
        String good = LauncherProfileCodec.encode(Collections.singleton(a), layout());
        rejects(good.replace("\"compact_rows\": false", "\"compact_rows\": \"false\""));
        rejects(good.replace("\"show_shelf\": true", "\"show_shelf\": null"));
        rejects(good.replace("\"layout\": {", "\"layout\": {\"untrusted\":true,"));
    }

    @Test public void rejectsDuplicateOrInvalidPinnedIdentifiers() throws Exception {
        String good = LauncherProfileCodec.encode(Collections.singleton(a), layout());
        rejects(good.replace("\""+a+"\"", "\""+a+"\", \""+a+"\""));
        rejects(good.replace(a, "bad/../../payload"));
        rejects(good.replace("\""+a+"\"", "17"));
    }

    @Test public void rejectsExtraRootFieldsAndOversizeInput() throws Exception {
        String good = LauncherProfileCodec.encode(Collections.singleton(a), layout());
        rejects(good.replace("\"schema\":", "\"extra\":true,\"schema\":"));
        byte[] oversized = new byte[LauncherProfileCodec.MAX_BYTES + 1];
        try { LauncherProfileCodec.decode(oversized, Collections.singleton(a)); fail("oversize accepted"); }
        catch (IllegalArgumentException expected) { assertNotNull(expected.getMessage()); }
    }

    @Test public void rejectsMalformedUtf8WithoutSilentlyReplacingBytes() throws Exception {
        try {
            LauncherProfileCodec.decode(new byte[]{(byte) 0xC3, (byte) 0x28},
                    Collections.singleton(a));
            fail("invalid UTF-8 accepted");
        } catch (java.nio.charset.CharacterCodingException expected) {
            assertNotNull(expected);
        }
    }

    @Test public void validatesExportBeforeWritingAnything() throws Exception {
        try { LauncherProfileCodec.encode(Collections.singleton("invalid"), layout()); fail(); }
        catch (IllegalArgumentException expected) { assertNotNull(expected); }
        Map<String, Boolean> incomplete = new LinkedHashMap<>(layout());
        incomplete.remove("show_shelf");
        try { LauncherProfileCodec.encode(Collections.singleton(a), incomplete); fail(); }
        catch (IllegalArgumentException expected) { assertNotNull(expected); }
    }

    private void rejects(String json) throws Exception {
        try {
            LauncherProfileCodec.decode(bytes(json), Collections.singleton(a));
            fail("accepted corrupt or unsupported profile: " + json);
        } catch (IllegalArgumentException | org.json.JSONException expected) {
            assertNotNull(expected);
        }
    }
}
