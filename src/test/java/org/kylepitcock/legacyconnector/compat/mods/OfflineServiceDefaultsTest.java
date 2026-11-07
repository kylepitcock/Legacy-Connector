package org.kylepitcock.legacyconnector.compat.mods;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OfflineServiceDefaultsTest {
    @Test
    void seedsOnlyForItsModAndOnlyWhenAbsent(@TempDir Path dir) throws Exception {
        OfflineServiceDefaults.beforePreInit("other", dir);
        assertFalse(Files.exists(dir.resolve("craftingdead")));
        OfflineServiceDefaults.beforePreInit("craftingdead", dir);
        Path f = dir.resolve("craftingdead/craftingdead.json");
        JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
        for (String k : new String[] {"master-server-host", "master-server-port", "display-presents", "servers", "website", "store", "maps", "discord"})
            assertTrue(o.has(k), k);
        JsonObject pack = o.getAsJsonObject("resource-pack");
        assertTrue(pack.has("url") && pack.has("hash"));
        assertDoesNotThrow(() -> new java.net.URI(pack.get("url").getAsString()).toURL());
        for (String k : new String[] {"network-address", "remote", "team"}) assertTrue(o.has(k), k);
        Files.writeString(f, "{\"website\":\"x\"}");
        OfflineServiceDefaults.beforePreInit("craftingdead", dir);
        JsonObject merged = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
        assertEquals("x", merged.get("website").getAsString());
        assertTrue(merged.has("network-address") && merged.has("servers"));
    }

    @Test
    void refreshesPlaceholderPackHashesSeededByEarlierVersions(@TempDir Path dir) throws Exception {
        OfflineServiceDefaults.beforePreInit("craftingdead", dir);
        Path f = dir.resolve("craftingdead/craftingdead.json");
        JsonObject good = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
        JsonObject old = good.deepCopy();
        old.getAsJsonObject("resource-pack").addProperty("hash", "00000000000000000000000000000000");
        old.getAsJsonObject("resource-pack").addProperty("url", "http://127.0.0.1:9/craftingdead.zip");
        Files.writeString(f, old.toString());
        OfflineServiceDefaults.beforePreInit("craftingdead", dir);
        assertEquals(good.getAsJsonObject("resource-pack"), JsonParser.parseString(Files.readString(f)).getAsJsonObject().getAsJsonObject("resource-pack"));
        // a pack entry pointing somewhere else is the user's own and stays
        old.getAsJsonObject("resource-pack").addProperty("url", "https://example.org/pack.zip");
        Files.writeString(f, old.toString());
        OfflineServiceDefaults.beforePreInit("craftingdead", dir);
        assertEquals("https://example.org/pack.zip", JsonParser.parseString(Files.readString(f)).getAsJsonObject().getAsJsonObject("resource-pack").get("url").getAsString());
    }
}
