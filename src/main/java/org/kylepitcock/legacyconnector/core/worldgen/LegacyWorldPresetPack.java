package org.kylepitcock.legacyconnector.core.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.pack.GeneratedTranslations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Version-neutral half of "legacy mod world types as 1.21 world presets" (the per-version adapters list the types of their shim's
 * {@code WorldType}: {@code v1_7_10.bridge.LegacyWorldPresets}, {@code v1_12_2.bridge.LegacyWorldPresets1122}; 1.4.7 to 1.6.4 ride the 1.7.10 one).
 * A preset is a native {@code worldgen/world_preset} listed on the Create World screen's world type button through the
 * {@code minecraft:normal} tag. Its overworld is the vanilla one with
 * <ul>
 * <li>its own copy of the noise settings, whose id identifies worlds of that type ({@link #presetId}); a legacy mod asking the world for its
 * {@code WorldType} gets the mod's type back,</li>
 * <li>optionally the type's own biome source, and/or the legacy chunk generator of the type's {@code getChunkGenerator}
 * ({@code legacyconnector:legacy_type}).</li>
 * </ul>
 */
public final class LegacyWorldPresetPack {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/WorldTypes");

    /** Every preset of every legacy version (one tag file, so the version adapters add to the same list). */
    private static final java.util.Set<String> LISTED = java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<>());

    private LegacyWorldPresetPack() {}

    /**
     * One world type.
     *
     * @param id the preset (and noise settings) id
     * @param typeName the legacy name ("BIOMESOP", "TFCDefault")
     * @param owner modern id of the legacy mod
     * @param translationKey legacy key of the display name ("generator.BIOMESOP")
     * @param generator properties merged over the vanilla overworld generator ("type", "biome_source", ...; null: the vanilla noise generator); "settings" is always the preset id
     */
    public record Preset(ResourceLocation id, String typeName, String owner, String translationKey, @Nullable JsonObject generator) {}

    /** Id of the preset of a mod's world type: {@code <mod>:<name>}, the name made a valid path. */
    public static ResourceLocation presetId(@Nullable String owner, String typeName) {
        return ResourceLocation.fromNamespaceAndPath(owner == null ? "legacyconnector" : owner, Ids.normalizePath(typeName).replace('/', '_'));
    }

    public static void contribute(Map<String, Supplier<byte[]>> out, List<Preset> presets) {
        if (presets.isEmpty()) return;
        JsonObject vanillaPreset = vanilla("data/minecraft/worldgen/world_preset/normal.json");
        JsonObject noise = vanilla("data/minecraft/worldgen/noise_settings/overworld.json");
        if (vanillaPreset == null || noise == null) {
            LOG.warn("Vanilla world preset data not found; legacy world types are not offered");
            return;
        }
        byte[] noiseBytes = noise.toString().getBytes(StandardCharsets.UTF_8);
        for (Preset t : presets) {
            ResourceLocation id = t.id();
            JsonObject p = vanillaPreset.deepCopy();
            JsonObject dims = p.getAsJsonObject("dimensions");
            JsonObject generator = dims.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator");
            if (t.generator() != null) for (var e : t.generator().entrySet()) generator.add(e.getKey(), e.getValue().deepCopy());
            generator.addProperty("settings", id.toString());
            byte[] presetBytes = p.toString().getBytes(StandardCharsets.UTF_8);
            out.put("data/" + id.getNamespace() + "/worldgen/world_preset/" + id.getPath() + ".json", () -> presetBytes);
            out.put("data/" + id.getNamespace() + "/worldgen/noise_settings/" + id.getPath() + ".json", () -> noiseBytes);
            LISTED.add(id.toString());
        }
        JsonArray tag = new JsonArray();
        for (String id : LISTED) tag.add(id);
        JsonObject tagJson = new JsonObject();
        tagJson.addProperty("replace", false);
        tagJson.add("values", tag);
        byte[] tagBytes = tagJson.toString().getBytes(StandardCharsets.UTF_8);
        out.put("data/minecraft/tags/worldgen/world_preset/normal.json", () -> tagBytes);
        LOG.info("Offering {} legacy world type(s) as world presets: {}", presets.size(), presets.stream().map(Preset::typeName).toList());
    }

    /** Button names: the mod's own translation of the legacy key (generator.BIOMESOP), else the type name (never throws). */
    public static void registerNames(List<Preset> presets) {
        for (Preset t : presets) {
            String name = null;
            try {
                name = legacyTranslation(t.owner(), t.translationKey());
            } catch (RuntimeException | LinkageError e) {
                LOG.debug("No translation of {}: {}", t.translationKey(), e.toString());
            }
            GeneratedTranslations.put("en_us", "generator." + t.id().getNamespace() + "." + t.id().getPath(), name == null ? t.typeName() : name);
        }
    }

    private static String legacyTranslation(String modId, String key) {
        var legacy = org.kylepitcock.legacyconnector.core.LegacyMods.find(modId);
        var file = legacy != null ? legacy.container.getModInfo().getOwningFile() : ModList.get().getModFileById(modId);
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
