package org.kylepitcock.legacyconnector.v1_12_2.advancement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.NeoForge;
import org.kylepitcock.legacyconnector.compat.patchouli.Ids1122;
import org.kylepitcock.legacyconnector.core.Ids;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.BiomeBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.ItemBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The advancements of 1.12.2 mods. 1.12 read them from {@code assets/<mod>/advancements/**.json} of the mod's own jar; the modern game reads
 * {@code data/<mod>/advancement/**.json} in another format. Each file is converted by {@link AdvancementConverter1122} (item subtypes, entity
 * types, recipes and biomes need the registries, so this runs when the data pack is built) and served by the bottom data pack, so world data packs
 * replace or remove them as any mod's advancement. Mods' own triggers go through {@link LegacyTrigger1122}.
 */
public final class Advancements1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Advancements1122");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static boolean registered;
    /** {@code namespace:path} of every converted advancement. */
    private static final Set<String> CONVERTED = new HashSet<>();
    private static final Set<String> REPORTED = new HashSet<>();

    private Advancements1122() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        GeneratedPack.addRecipeContributor(Advancements1122::contribute);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> report(e.getServer()));
    }

    /** Resolver over the legacy registries. */
    static final AdvancementConverter1122.Resolver RESOLVER = new AdvancementConverter1122.Resolver() {
        @Override
        public List<String> items(String id, Integer data) {
            List<String> out = new ArrayList<>();
            try {
                var item = Registries1122.ITEMS.getValue(new ResourceLocation(id));
                if (item == null) return out;
                if (data != null) {
                    add(out, ItemBridge1122.modern(item, data));
                } else {
                    var variants = ItemBridge1122.variants(item);
                    if (variants.isEmpty()) add(out, ItemBridge1122.modern(item, 0));
                    else variants.values().forEach(m -> add(out, m));
                }
            } catch (RuntimeException | LinkageError e) {
                // unknown
            }
            return out;
        }

        private void add(List<String> out, net.minecraft.world.item.Item item) {
            if (item == null) return;
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (!out.contains(id)) out.add(id);
        }

        @Override
        public String block(String id) {
            try {
                var block = Registries1122.BLOCKS.getValue(new ResourceLocation(id));
                var modern = block == null ? null : Ids1122.modern(block);
                return modern == null ? null : BuiltInRegistries.BLOCK.getKey(modern).toString();
            } catch (RuntimeException | LinkageError e) {
                return null;
            }
        }

        @Override
        public String entity(String id) {
            String modern = Ids1122.entity(id.indexOf(':') < 0 ? "minecraft:" + id : id);
            var rl = net.minecraft.resources.ResourceLocation.tryParse(modern);
            return rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl) ? modern : null;
        }

        @Override
        public String effect(String id) {
            String n = Ids.normalizePath(id.indexOf(':') < 0 ? "minecraft:" + id : id).replace('/', '_');
            int c = n.indexOf(':');
            String modern = Ids.modernModId(n.substring(0, c)) + ":" + n.substring(c + 1);
            var rl = net.minecraft.resources.ResourceLocation.tryParse(modern);
            return rl != null && BuiltInRegistries.MOB_EFFECT.containsKey(rl) ? modern : null;
        }

        @Override
        public String recipe(String id) {
            int c = id.indexOf(':');
            String ns = c < 0 ? "minecraft" : id.substring(0, c);
            String path = c < 0 ? id : id.substring(c + 1);
            if (ns.equals("minecraft")) return "minecraft:" + Ids.normalizePath(path);
            return Ids.modernModId(ns) + ":" + Ids.normalizePath(path).replace('/', '_') + "_code";
        }

        @Override
        public String biome(String id) {
            try {
                var biome = Registries1122.BIOMES.getValue(new ResourceLocation(id));
                return biome == null ? null : BiomeBridge1122.modernId(biome).toString();
            } catch (RuntimeException | LinkageError e) {
                return null;
            }
        }

        @Override
        public List<String> structures(String name) {
            String n = name.toLowerCase(Locale.ROOT);
            if (n.startsWith("minecraft:")) n = n.substring("minecraft:".length());
            return switch (n) {
                case "village" -> List.of("minecraft:village_plains", "minecraft:village_desert", "minecraft:village_savanna", "minecraft:village_snowy",
                        "minecraft:village_taiga");
                case "mineshaft" -> List.of("minecraft:mineshaft", "minecraft:mineshaft_mesa");
                case "temple" -> List.of("minecraft:desert_pyramid", "minecraft:jungle_pyramid", "minecraft:swamp_hut", "minecraft:igloo");
                case "stronghold" -> List.of("minecraft:stronghold");
                case "monument" -> List.of("minecraft:ocean_monument");
                case "mansion" -> List.of("minecraft:mansion");
                case "endcity" -> List.of("minecraft:end_city");
                case "fortress" -> List.of("minecraft:fortress");
                default -> List.of();
            };
        }

        @Override
        public String dimension(String name) {
            String n = name.toLowerCase(Locale.ROOT);
            switch (n) {
                case "overworld", "minecraft:overworld" -> {
                    return "minecraft:overworld";
                }
                case "the_nether", "nether", "minecraft:the_nether" -> {
                    return "minecraft:the_nether";
                }
                case "the_end", "the end", "minecraft:the_end" -> {
                    return "minecraft:the_end";
                }
                default -> {
                }
            }
            try {
                for (var e : org.kylepitcock.legacyconnector.v1_12_2.dimension.Dimensions1122.modDimensions().entrySet()) {
                    String typeName = e.getValue().getName();
                    if (typeName != null && (typeName.equalsIgnoreCase(name) || typeName.replace(' ', '_').equalsIgnoreCase(name))) {
                        return org.kylepitcock.legacyconnector.v1_12_2.dimension.Dimensions1122.id(e.getKey()).toString();
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                // no dimension manager state
            }
            return null;
        }

        @Override
        public String resource(String id) {
            return Ids1122.resource(id);
        }
    };

    private static void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.SERVER_DATA) return;
        CONVERTED.clear();
        Map<String, AdvancementConverter1122.Result> converted = new LinkedHashMap<>();
        int failed = 0;
        int custom = 0;
        for (Entry entry : files()) {
            try {
                JsonReader reader = new JsonReader(new StringReader(Files.readString(entry.file, StandardCharsets.UTF_8).replace("﻿", "")));
                reader.setLenient(true);
                JsonElement root0 = JsonParser.parseReader(reader);
                if (!root0.isJsonObject()) continue;
                // Forge 1.12 advancement conditions ("forge:mod_loaded" and the like): files with a failing condition are skipped by Forge
                if (root0.getAsJsonObject().has("conditions") && !conditionsHold(root0.getAsJsonObject().get("conditions"))) continue;
                converted.put(entry.key, AdvancementConverter1122.convert(root0.getAsJsonObject(), RESOLVER));
            } catch (IOException | RuntimeException | LinkageError e) {
                failed++;
                LOG.warn("Cannot convert the 1.12.2 advancement {}: {}", entry.key, e.toString());
            }
        }
        if (converted.isEmpty() && failed == 0) return;
        int impossible = 0;
        for (var entry : converted.entrySet()) {
            var result = entry.getValue();
            if (result.parent != null && !result.parent.startsWith("minecraft:") && !converted.containsKey(result.parent)) {
                result.json.remove("parent"); // the modern game drops an advancement whose parent does not exist
                result.notes.add("parent " + result.parent + " does not exist (made a root)");
            }
            for (String note : result.notes) {
                if (REPORTED.add(note)) LOG.info("1.12.2 advancements: {} (first in {})", note, entry.getKey());
            }
            String text = GSON.toJson(result.json);
            if (text.contains("minecraft:impossible")) impossible++;
            if (text.contains(AdvancementConverter1122.LEGACY_TRIGGER)) custom++;
            String key = entry.getKey();
            int colon = key.indexOf(':');
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            out.put("data/" + key.substring(0, colon) + "/advancement/" + key.substring(colon + 1) + ".json", () -> bytes);
            CONVERTED.add(key);
        }
        LOG.info("1.12.2 advancements of mod jars: {} converted, {} failed to convert; {} use a trigger of a mod, {} contain a criterion that can not be met here",
                converted.size(), failed, custom, impossible);
    }

    private record Entry(String key, Path file) {}

    private static volatile Set<String> knownIds;

    /** Whether a 1.12.2 mod ships an advancement file of this modern id ({@code namespace:path}). */
    public static boolean known(String id) {
        Set<String> ids = knownIds;
        if (ids == null) {
            ids = new HashSet<>();
            for (Entry e : files()) ids.add(e.key);
            if (!LegacyMods.all().isEmpty()) knownIds = ids;
        }
        return ids.contains(id);
    }

    /** Every advancement file of the 1.12.2 mod jars, with its modern id. */
    private static List<Entry> files() {
        List<Entry> out = new ArrayList<>();
        Set<Object> seen = new HashSet<>();
        for (LegacyModRecord mod : LegacyMods.all()) {
            if (!"1.12.2".equals(mod.version) || mod.container == null) continue;
            var file = mod.container.getModInfo().getOwningFile().getFile();
            if (!seen.add(file)) continue;
            Path assets;
            try {
                assets = file.findResource("assets");
            } catch (RuntimeException e) {
                continue;
            }
            if (assets == null || !Files.isDirectory(assets)) continue;
            try (Stream<Path> namespaces = Files.list(assets)) {
                for (Path nsDir : (Iterable<Path>) namespaces.filter(p -> Files.isDirectory(p.resolve("advancements")))::iterator) {
                    String ns = Ids.normalizePath(nsDir.getFileName().toString());
                    Path root = nsDir.resolve("advancements");
                    try (Stream<Path> walk = Files.walk(root)) {
                        for (Path f : (Iterable<Path>) walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json")).sorted()::iterator) {
                            String rel = root.relativize(f).toString().replace('\\', '/');
                            String name = Ids.normalizePath(rel.substring(0, rel.length() - ".json".length()));
                            if (name.startsWith("_") || name.contains("/_")) continue; // 1.12 skipped underscore-prefixed files
                            out.add(new Entry(ns + ":" + name, f));
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOG.warn("Cannot read the advancements of {}: {}", mod.legacyId, e.toString());
            }
        }
        return out;
    }

    /** Whether the {@code conditions} array of a file holds (only a missing mod fails it; unknown condition types hold). */
    private static boolean conditionsHold(JsonElement conditions) {
        if (!conditions.isJsonArray()) return true;
        for (JsonElement c : conditions.getAsJsonArray()) {
            if (!c.isJsonObject()) continue;
            JsonObject o = c.getAsJsonObject();
            String type = o.has("type") ? o.get("type").getAsString() : "";
            if (type.equals("forge:mod_loaded") && o.has("modid") && LegacyMods.find(o.get("modid").getAsString()) == null
                    && !net.neoforged.fml.ModList.get().isLoaded(o.get("modid").getAsString())) return false;
        }
        return true;
    }

    private static void report(net.minecraft.server.MinecraftServer server) {
        if (CONVERTED.isEmpty()) return;
        int loaded = 0;
        List<String> missing = new ArrayList<>();
        for (String key : CONVERTED) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(key);
            if (id != null && server.getAdvancements().get(id) != null) loaded++;
            else if (missing.size() < 5) missing.add(key);
        }
        LOG.info("1.12.2 advancements loaded by the server: {} of {} converted{}", loaded, CONVERTED.size(),
                missing.isEmpty() ? "" : " (rejected, see the errors above: " + String.join(", ", missing) + (CONVERTED.size() - loaded > missing.size() ? ", ..." : "") + ")");
    }
}
