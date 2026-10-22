package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import legacyconnector.v1_7_10.net.minecraft.world.WorldType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.kylepitcock.legacyconnector.core.pack.GeneratedTranslations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy mod world types (Biomes O' Plenty's "BIOMESOP", ...) as 1.21 world presets, listed on the Create World
 * screen's world type button. Each preset is the vanilla overworld with its own copy of the noise settings, whose id
 * tells {@link LegacyBiomeLayers} which legacy world type's biome layers to use for the world.
 */
public final class LegacyWorldPresets {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Biomes");

    private LegacyWorldPresets() {}

    /** World types registered by legacy mods. */
    public static List<WorldType> modTypes() {
        List<WorldType> out = new ArrayList<>();
        for (WorldType t : WorldType.worldTypes) if (t != null && t.mcc$owner != null && t.getCanBeCreated()) out.add(t);
        return out;
    }

    public static WorldType bySettings(ResourceLocation noiseSettings) {
        for (WorldType t : modTypes()) if (t.mcc$presetId().equals(noiseSettings)) return t;
        return null;
    }

    public static void contributeData(Map<String, Supplier<byte[]>> out) {
        List<WorldType> types = modTypes();
        if (types.isEmpty()) return;
        JsonObject preset = vanilla("data/minecraft/worldgen/world_preset/normal.json");
        JsonObject noise = vanilla("data/minecraft/worldgen/noise_settings/overworld.json");
        if (preset == null || noise == null) {
            LOG.warn("Vanilla world preset data not found; legacy world types are not offered");
            return;
        }
        byte[] noiseBytes = noise.toString().getBytes(StandardCharsets.UTF_8);
        JsonArray tag = new JsonArray();
        for (WorldType t : types) {
            ResourceLocation id = t.mcc$presetId();
            JsonObject p = preset.deepCopy();
            p.getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator")
                    .addProperty("settings", id.toString());
            byte[] presetBytes = p.toString().getBytes(StandardCharsets.UTF_8);
            out.put("data/" + id.getNamespace() + "/worldgen/world_preset/" + id.getPath() + ".json", () -> presetBytes);
            out.put("data/" + id.getNamespace() + "/worldgen/noise_settings/" + id.getPath() + ".json", () -> noiseBytes);
            tag.add(id.toString());
        }
        JsonObject tagJson = new JsonObject();
        tagJson.addProperty("replace", false);
        tagJson.add("values", tag);
        byte[] tagBytes = tagJson.toString().getBytes(StandardCharsets.UTF_8);
        out.put("data/minecraft/tags/worldgen/world_preset/normal.json", () -> tagBytes);
        LOG.info("Offering {} legacy world type(s) as world presets", types.size());
    }

    /** Display names: the mod's own translation of the legacy key (generator.BIOMESOP), else the type name. */
    public static void registerNames() {
        for (WorldType t : modTypes()) {
            ResourceLocation id = t.mcc$presetId();
            String name = legacyTranslation(t.mcc$owner, t.getTranslateName());
            GeneratedTranslations.put("en_us", "generator." + id.getNamespace() + "." + id.getPath(), name == null ? t.getWorldTypeName() : name);
        }
    }

    private static String legacyTranslation(String modId, String key) {
        var file = ModList.get().getModFileById(modId);
        if (file == null) return null;
        Path assets = file.getFile().getSecureJar().getPath("assets");
        if (!Files.isDirectory(assets)) return null;
        try (Stream<Path> langs = Files.find(assets, 3, (p, a) -> p.getFileName() != null && p.getFileName().toString().equals("en_us.json"))) {
            for (Path lang : (Iterable<Path>) langs::iterator) {
                try (var r = Files.newBufferedReader(lang, StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
                    if (json.has(key)) return json.get(key).getAsString();
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not read translations of {}: {}", modId, e.toString());
        }
        return null;
    }

    private static JsonObject vanilla(String path) {
        for (ClassLoader cl : new ClassLoader[] {net.minecraft.world.level.biome.Biome.class.getClassLoader(), Thread.currentThread().getContextClassLoader()}) {
            if (cl == null) continue;
            try (InputStream in = cl.getResourceAsStream(path)) {
                if (in != null) return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception ignored) {
                // try the next loader
            }
        }
        return null;
    }
}
