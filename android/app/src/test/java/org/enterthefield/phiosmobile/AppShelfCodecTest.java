package org.enterthefield.phiosmobile;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class AppShelfCodecTest {
    private AppShelfCodec.Catalog catalog() throws Exception {
        return AppShelfCodec.readCatalog(Files.readAllBytes(
                Path.of("src", "main", "assets", "public-repo-catalog.v1.json")));
    }

    private byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }

    @Test public void exactPublicSnapshotAndNativeSourceOnlyAuthority() throws Exception {
        AppShelfCodec.Catalog catalog = catalog();
        assertEquals(225, catalog.sources.size());
        assertEquals("e295f6265e60032de98a2acd8e53295505fc47e8", catalog.sha);
        assertTrue(catalog.sources.containsKey("github:1312447255"));
        assertEquals("https://github.com/MichaelWave369/12fold",
                catalog.sources.get("github:1312447255").repository);
    }

    @Test public void webMobileAndNativeWireRoundTripKeepsOrder() throws Exception {
        AppShelfCodec.Catalog catalog = catalog();
        List<String> pins = Arrays.asList("github:1312447255", "github:1167870912");
        String json = AppShelfCodec.encode(pins, catalog, Instant.parse("2026-11-05T22:00:00.000Z"));
        JSONObject packet = new JSONObject(json);
        assertEquals("phios-app-shelf", packet.getString("format"));
        assertEquals(1, packet.getInt("version"));
        assertEquals("2026-11-05T22:00:00.000Z", packet.getString("exported_at"));
        assertEquals(5, packet.length());
        assertEquals(pins, AppShelfCodec.decode(bytes(json), catalog));
    }

    @Test public void rejectsInjectedAuthorityExtraFieldsAndSnapshotMismatch() throws Exception {
        AppShelfCodec.Catalog catalog = catalog();
        JSONObject packet = new JSONObject(AppShelfCodec.encode(
                Collections.singletonList("github:1312447255"), catalog, Instant.now()));
        packet.put("install_authority", true);
        rejects(packet.toString(), catalog);
        packet.remove("install_authority");
        packet.put("catalog_source_sha", "0".repeat(40));
        rejects(packet.toString(), catalog);
        packet.put("catalog_source_sha", AppShelfCodec.SOURCE_SHA);
        packet.put("version", "1");
        rejects(packet.toString(), catalog);
        packet.put("version", 1);
        packet.put("pinned", new JSONArray().put("github:1312447255").put("github:1312447255"));
        rejects(packet.toString(), catalog);
        packet.put("pinned", new JSONArray().put("github:999999999999999"));
        rejects(packet.toString(), catalog);
    }

    @Test public void rejectsBadUtf8OversizeAndTooManyPins() throws Exception {
        AppShelfCodec.Catalog catalog = catalog();
        try {
            AppShelfCodec.decode(new byte[]{(byte) 0xC3, 0x28}, catalog);
            fail("accepted malformed UTF-8");
        } catch (java.nio.charset.CharacterCodingException expected) { assertNotNull(expected); }
        byte[] oversized = new byte[AppShelfCodec.MAX_BYTES + 1];
        try {
            AppShelfCodec.decode(oversized, catalog);
            fail("oversize accepted");
        } catch (IllegalArgumentException expected) { assertNotNull(expected); }
        List<String> all = new ArrayList<>(catalog.sources.keySet());
        try {
            AppShelfCodec.validate(all.subList(0, 25), catalog);
            fail("accepted 25 pins");
        } catch (IllegalArgumentException expected) { assertNotNull(expected); }
    }

    @Test public void refusesPrivilegedCatalogAndWrongOwner() throws Exception {
        byte[] raw=Files.readAllBytes(Path.of("src", "main", "assets", "public-repo-catalog.v1.json"));
        JSONObject packet = new JSONObject(new String(raw, StandardCharsets.UTF_8));
        packet.getJSONObject("policy").put("automatic_install", true);
        rejectsCatalog(packet);
        packet.getJSONObject("policy").put("automatic_install", false);
        packet.getJSONObject("provenance").put("scope", "private");
        rejectsCatalog(packet);
    }

    private void rejects(String json, AppShelfCodec.Catalog catalog) throws Exception {
        try {
            AppShelfCodec.decode(bytes(json), catalog);
            fail("accepted unsafe or unsupported shelf JSON");
        } catch (IllegalArgumentException | org.json.JSONException expected) {
            assertNotNull(expected);
        }
    }

    private void rejectsCatalog(JSONObject value) throws Exception {
        try {
            AppShelfCodec.readCatalog(bytes(value.toString()));
            fail("accepted untrusted source catalog");
        } catch (IllegalArgumentException | org.json.JSONException expected) {
            assertNotNull(expected);
        }
    }
}
