package org.kylepitcock.legacyconnector.compat.mods;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MOD-SPECIFIC (Crafting Dead 1.3.5, mod id {@code craftingdead}): the mod downloads its service settings
 * ({@code craftingdead/craftingdead.json}: master server, resource pack, website links) from a host that no longer exists. Without
 * the file its client preInit continues with a null resource pack URL and dies with a NullPointerException in the downloader
 * ({@code com.craftingdead.k.e.a(File, URL, String)}). As in Forge the download is still tried first; only when the mod's settings
 * file is absent, this seeds an offline one (no servers, links to the local host, a pack URL that refuses the connection, which
 * the mod's downloader logs and survives). Remove this class and its one call in {@code Platform1710.preInit} to drop the fix.
 */
public final class OfflineServiceDefaults {
    static final String MOD = "craftingdead";

    static final String JSON = """
            {
              "master-server-host": "localhost",
              "master-server-port": 1,
              "display-presents": false,
              "resource-pack": {"url": "http://127.0.0.1:9/craftingdead.zip", "hash": "00000000000000000000000000000000"},
              "servers": {},
              "website": "http://localhost/",
              "store": "http://localhost/",
              "maps": "http://localhost/",
              "discord": "http://localhost/"
            }
            """;

    private OfflineServiceDefaults() {}

    /** Called before the mod's preInit; {@code gameDir} is the directory the mod resolves {@code user.dir} to. */
    public static void beforePreInit(String modId, Path gameDir) {
        if (!MOD.equals(modId)) return;
        try {
            Path file = gameDir.resolve("craftingdead").resolve("craftingdead.json");
            if (Files.exists(file)) return;
            Files.createDirectories(file.getParent());
            Files.writeString(file, JSON, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // the mod then fails as it did before
        }
    }
}
