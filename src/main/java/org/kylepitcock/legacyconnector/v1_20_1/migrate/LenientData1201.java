package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.fml.ModList;
import org.kylepitcock.legacyconnector.core.degraded.Degraded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data-pack entries of translated 1.20.1 mods that name an object no installed mod registers. Forge 1.20.1 skipped an unknown entity id in
 * a biome modifier's spawner list; NeoForge 1.21.1 fails the whole data-pack registry load, so no world can be created. Whether an id exists
 * depends on the other installed mods, so the check runs when the data pack opens (every registry is complete by then): the offending
 * entries are dropped (the whole modifier becomes {@code neoforge:none} when nothing valid is left) in a copy served by the layer's data pack
 * above the mod's own, and each drop is reported once through {@link Degraded}.
 */
public final class LenientData1201 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/LenientData");

    private LenientData1201() {}

    /** The result of {@link #filterBiomeModifier}: the replacement file and the ids that were dropped. */
    public record Filtered(JsonObject replacement, List<String> dropped) {}

    /**
     * Filters a converted (1.21.1) biome modifier. Returns null when it needs no change.
     * {@code neoforge:add_spawns}: spawner entries whose {@code type} is unknown go; {@code neoforge:remove_spawns}: unknown {@code entity_types} go.
     */
    public static Filtered filterBiomeModifier(JsonObject modifier, Predicate<String> knownEntity) {
        JsonElement type = modifier.get("type");
        if (type == null || !type.isJsonPrimitive()) return null;
        List<String> dropped = new ArrayList<>();
        String t = type.getAsString();
        if (t.equals("neoforge:add_spawns") && modifier.has("spawners")) {
            JsonElement spawners = modifier.get("spawners");
            JsonArray kept = new JsonArray();
            for (JsonElement s : spawners.isJsonArray() ? spawners.getAsJsonArray() : singleton(spawners)) {
                String id = s.isJsonObject() && s.getAsJsonObject().has("type") && s.getAsJsonObject().get("type").isJsonPrimitive()
                        ? s.getAsJsonObject().get("type").getAsString() : null;
                if (id != null && !knownEntity.test(id)) dropped.add(id);
                else kept.add(s);
            }
            if (dropped.isEmpty()) return null;
            if (kept.isEmpty()) return new Filtered(none(), dropped);
            JsonObject copy = modifier.deepCopy();
            copy.add("spawners", spawners.isJsonArray() ? kept : kept.get(0));
            return new Filtered(copy, dropped);
        }
        if (t.equals("neoforge:remove_spawns") && modifier.has("entity_types")) {
            JsonElement types = modifier.get("entity_types");
            JsonArray kept = new JsonArray();
            for (JsonElement e : types.isJsonArray() ? types.getAsJsonArray() : singleton(types)) {
                String id = e.isJsonPrimitive() ? e.getAsString() : null;
                if (id != null && !id.startsWith("#") && !knownEntity.test(id)) dropped.add(id);
                else kept.add(e);
            }
            if (dropped.isEmpty()) return null;
            if (kept.isEmpty()) return new Filtered(none(), dropped);
            JsonObject copy = modifier.deepCopy();
            copy.add("entity_types", types.isJsonArray() ? kept : kept.get(0));
            return new Filtered(copy, dropped);
        }
        return null;
    }

    private static JsonArray singleton(JsonElement e) {
        JsonArray a = new JsonArray();
        a.add(e);
        return a;
    }

    private static JsonObject none() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "neoforge:none");
        return o;
    }

    private static boolean knownEntity(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl);
    }

    /** A {@link org.kylepitcock.legacyconnector.core.pack.GeneratedPack.Contributor}: serves the filtered copies of the mods' biome modifiers. */
    public static void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.SERVER_DATA) return;
        ModList list = ModList.get();
        if (list == null) return;
        for (String modId : org.kylepitcock.legacyconnector.v1_20_1.Forge1201Runtime.modIds()) {
            try {
                var info = list.getModFileById(modId);
                if (info == null) continue;
                Path data = info.getFile().findResource("data");
                if (!Files.isDirectory(data)) continue;
                List<Path> namespaces;
                try (Stream<Path> s = Files.list(data)) {
                    namespaces = s.filter(Files::isDirectory).toList();
                }
                for (Path ns : namespaces) {
                    Path dir = ns.resolve("neoforge").resolve("biome_modifier");
                    if (!Files.isDirectory(dir)) continue;
                    String nsName = ns.getFileName().toString().replace("/", "");
                    try (Stream<Path> files = Files.walk(dir)) {
                        for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                            filterFile(modId, nsName, dir, f, out);
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOG.warn("could not check the biome modifiers of {}: {}", modId, e.toString());
            }
        }
    }

    private static void filterFile(String modId, String ns, Path dir, Path file, Map<String, Supplier<byte[]>> out) {
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(r);
            if (!root.isJsonObject()) return;
            Filtered f = filterBiomeModifier(root.getAsJsonObject(), LenientData1201::knownEntity);
            if (f == null) return;
            String rel = dir.relativize(file).toString().replace('\\', '/');
            String path = "data/" + ns + "/neoforge/biome_modifier/" + rel;
            byte[] bytes = f.replacement().toString().getBytes(StandardCharsets.UTF_8);
            out.put(path, () -> bytes);
            boolean whole = f.replacement().has("type") && f.replacement().get("type").getAsString().equals("neoforge:none");
            Degraded.note(Degraded.Category.OTHER, modId, path,
                    "biome modifier " + ns + ":" + rel + " names unknown entity types " + f.dropped() + " (Forge 1.20.1 skipped them): "
                            + (whole ? "the modifier is disabled" : "those entries are dropped"));
        } catch (IOException | RuntimeException e) {
            LOG.warn("could not check biome modifier {}: {}", file, e.toString());
        }
    }
}
