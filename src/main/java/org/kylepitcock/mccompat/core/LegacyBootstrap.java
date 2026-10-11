package org.kylepitcock.mccompat.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Called from the {@code @Mod} entrypoint class that the loader generates into every translated jar.
 *
 * <p>FML constructs mods in parallel, which legacy mods were never written for (1.7.10 FML was strictly
 * sequential). So this only records the mod; {@link LegacyLifecycle} instantiates legacy mod classes later,
 * one by one in dependency order, on the main thread.</p>
 */
public final class LegacyBootstrap {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit");

    private LegacyBootstrap() {}

    public static void construct(String version, String legacyId, String mainClass, IEventBus modBus, ModContainer container) {
        JsonObject meta = readMetadata(container, legacyId);
        LegacyModRecord record = new LegacyModRecord(version, legacyId, mainClass, container, modBus, meta);
        LegacyMods.add(record);
        LOG.info("Discovered legacy {} mod {} ({}) as {}", version, legacyId, mainClass, record.modernId);
    }

    private static JsonObject readMetadata(ModContainer container, String legacyId) {
        try {
            Path path = container.getModInfo().getOwningFile().getFile().findResource("META-INF", "mccompat", "legacy-mods.json");
            if (!Files.exists(path)) return null;
            try (InputStream in = Files.newInputStream(path)) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (JsonElement e : root.getAsJsonArray("mods")) {
                    if (e.getAsJsonObject().get("legacyId").getAsString().equals(legacyId)) return e.getAsJsonObject();
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not read legacy metadata for {}", legacyId, e);
        }
        return null;
    }
}
