package org.kylepitcock.legacyconnector.core.worldgen;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;

/**
 * Legacy world generators (1.7.10 and 1.12.2 {@code IWorldGenerator}s) as native world generation: each one becomes a configured and a
 * placed feature {@code <modid>:legacy_worldgen/<generator>} (type {@code legacyconnector:legacy_generator}, see
 * {@link LegacyGeneratorFeature}), and one NeoForge biome modifier, {@code legacyconnector:legacy_worldgen}, adds them to every biome
 * in the {@code top_layer_modification} step: after vanilla decoration, as FML ran the generators after the chunk provider's populate.
 * The modifier lists them in legacy order (GameRegistry weight, then registration order) after {@code legacyconnector:legacy_worldgen}
 * (the populate terrain events, and any generator registered after the pack was built), and the features of one step run in the order
 * a biome lists them, so the legacy order holds. Data packs and other mods can remove, replace or retune single generators by id
 * ({@code neoforge:remove_features}, or the tag {@code #legacyconnector:legacy_generators} for all of them).
 *
 * <p>Ids are derived from the generator's class (snake case of its simple name, {@code _2}, {@code _3}... for further instances of
 * the same class in registration order) under the namespace of the legacy mod whose jar the class is in, so they are stable across
 * launches with the same mods.
 */
public final class LegacyGenerators {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/WorldGen");

    /** Runs one legacy generator for one chunk with the legacy seeding and the mod's lock; may throw (the feature catches it). */
    public interface Runner {
        void run(WorldGenLevel level, int chunkX, int chunkZ);
    }

    /** A legacy generator: the legacy object (identity is the key), the legacy mod id that owns it (null: unknown) and how to run it. */
    public record Generator(Object generator, String modId, Runner runner) {}

    /** A legacy family's generators, in the order the legacy loader ran them. */
    public interface Source {
        List<Generator> generators();
    }

    private static final List<Source> SOURCES = new CopyOnWriteArrayList<>();
    /** Assigned ids, by generator identity: an id never changes in a session (pack rebuilds, /reload). */
    private static final Map<Object, ResourceLocation> IDS = new IdentityHashMap<>();
    private static final Map<ResourceLocation, Generator> BY_ID = new ConcurrentHashMap<>();

    public static final String MODIFIER = "legacyconnector:legacy_worldgen";
    public static final String TAG = "legacyconnector:legacy_generators";
    public static final String STEP = "top_layer_modification";

    private LegacyGenerators() {}

    public static void addSource(Source source) {
        if (!SOURCES.contains(source)) SOURCES.add(source);
    }

    /** The generator a {@code legacyconnector:legacy_generator} feature names, or null. */
    static Generator byId(ResourceLocation id) {
        return BY_ID.get(id);
    }

    /** The id of a legacy generator's feature, or null when it has none (registered after the generated pack was built). */
    public static ResourceLocation idOf(Object generator) {
        synchronized (IDS) {
            return IDS.get(generator);
        }
    }

    /**
     * Whether a legacy generator runs as its own placed feature in this world (its id is in the world's placed feature registry; a data
     * pack that removed it from the biomes still counts: then it is meant not to run). The catch-all feature runs the others.
     */
    public static boolean covered(WorldGenLevel level, Object generator) {
        ResourceLocation id = idOf(generator);
        if (id == null) return false;
        try {
            return level.registryAccess().registry(Registries.PLACED_FEATURE).map(r -> r.containsKey(id)).orElse(false);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** All generators of all families in legacy order, with their ids (assigned on first sight). */
    private static List<Map.Entry<ResourceLocation, Generator>> snapshot() {
        List<Map.Entry<ResourceLocation, Generator>> out = new ArrayList<>();
        synchronized (IDS) {
            for (Source s : SOURCES) {
                List<Generator> gens;
                try {
                    gens = s.generators();
                } catch (RuntimeException | LinkageError e) {
                    LOG.error("Listing legacy world generators failed ({})", s, e);
                    continue;
                }
                for (Generator g : gens) {
                    if (g == null || g.generator() == null) continue;
                    ResourceLocation id = IDS.get(g.generator());
                    if (id == null) {
                        id = newId(g);
                        IDS.put(g.generator(), id);
                    }
                    BY_ID.put(id, g);
                    out.add(Map.entry(id, g));
                }
            }
        }
        return out;
    }

    private static ResourceLocation newId(Generator g) {
        String namespace = namespace(g.modId());
        String base = "legacy_worldgen/" + baseName(g.generator().getClass());
        ResourceLocation id = ResourceLocation.tryBuild(namespace, base);
        if (id == null) id = ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_worldgen/generator");
        for (int n = 2; IDS.containsValue(id); n++) id = id.withPath(base + "_" + n);
        return id;
    }

    private static String namespace(String modId) {
        if (modId == null) return "legacyconnector";
        String ns = modId.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return ns.isEmpty() || ns.equals("minecraft") ? "legacyconnector" : ns;
    }

    /** {@code ThaumcraftWorldGenerator} -> {@code thaumcraft_world_generator}; nested {@code Outer$Inner} -> {@code outer_inner}. */
    static String baseName(Class<?> type) {
        String name = type.getName();
        int lambda = name.indexOf("$$Lambda");
        if (lambda >= 0) name = name.substring(0, lambda) + "$lambda";
        name = name.substring(name.lastIndexOf('.') + 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                char prev = i > 0 ? name.charAt(i - 1) : '_';
                char next = i + 1 < name.length() ? name.charAt(i + 1) : '_';
                if (Character.isLowerCase(prev) || Character.isDigit(prev) || (Character.isUpperCase(prev) && Character.isLowerCase(next))) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String out = sb.toString().replaceAll("_+", "_").replaceAll("^_|_$", "");
        return out.isEmpty() ? "generator" : out;
    }

    /**
     * Writes the features of every legacy generator, the tag of them all and the biome modifier that adds the catch-all feature and
     * then each generator, in legacy order, to every biome (called by the core data contributor while the generated pack is built).
     */
    public static void contribute(Map<String, Supplier<byte[]>> out) {
        StringBuilder features = new StringBuilder("\"").append(MODIFIER).append('"');
        StringBuilder tag = new StringBuilder();
        // a feature listed twice in one step is a feature order cycle in every biome (vanilla's indexer throws): each id once
        java.util.Set<ResourceLocation> seen = new java.util.HashSet<>();
        for (var e : snapshot()) {
            ResourceLocation id = e.getKey();
            if (!seen.add(id)) continue;
            String dir = "data/" + id.getNamespace() + "/worldgen/";
            put(out, dir + "configured_feature/" + id.getPath() + ".json",
                    "{\"type\":\"legacyconnector:legacy_generator\",\"config\":{\"generator\":\"" + id + "\"}}");
            put(out, dir + "placed_feature/" + id.getPath() + ".json", "{\"feature\":\"" + id + "\",\"placement\":[]}");
            features.append(",\"").append(id).append('"');
            if (tag.length() > 0) tag.append(',');
            tag.append('"').append(id).append('"');
        }
        put(out, "data/legacyconnector/tags/worldgen/placed_feature/legacy_generators.json", "{\"values\":[" + tag + "]}");
        put(out, "data/legacyconnector/neoforge/biome_modifier/legacy_worldgen.json",
                "{\"type\":\"neoforge:add_features\",\"biomes\":{\"type\":\"neoforge:any\"},\"features\":[" + features + "],\"step\":\"" + STEP + "\"}");
    }

    private static void put(Map<String, Supplier<byte[]>> out, String path, String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        out.put(path, () -> bytes);
    }
}
