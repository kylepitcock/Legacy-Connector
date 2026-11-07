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
        Files.writeString(f, "{}");
        OfflineServiceDefaults.beforePreInit("craftingdead", dir);
        assertEquals("{}", Files.readString(f));
    }
}
