package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import legacyconnector.v1_7_10.cpw.mods.fml.common.IWorldGenerator;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.WorldGenLevel;
import org.kylepitcock.legacyconnector.core.LegacyConfig;
import org.kylepitcock.legacyconnector.core.LegacyModLocks;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The deep layer of 1.7.10 world generators. 1.7.10 worlds ended at y=0; modern ones go down to -64 (deepslate, bedrock).
 * Every legacy {@code IWorldGenerator} runs a second time per chunk with the world shifted ({@link World#mcc$enterDeepPass}),
 * so that legacy y=0 is the dimension's bottom: ores, bedrock ores and underground structures spawn in the deep layer too.
 *
 * <p>Each generator's deep pass is a native placed feature, {@code <modid>:legacy_worldgen_deep/<generator>}, added to every
 * biome by a biome modifier of the same id (step {@code underground_ores}). Its placement is a {@code minecraft:height_range}
 * whose y is where legacy y=0 lands (default: the dimension's bottom); the window it generates into reaches from there up to
 * modern y=0 (the translation buries everything above). A placement at or above y=0 (the Nether, the End) runs nothing.
 * Data packs remove a generator's deep pass (replace its biome modifier with {@code neoforge:none}) or move it (change the
 * height). The config keys {@code deepLayer.enabled} / {@code deepLayer.excludeGenerators} decide which features are generated.
 *
 * <p>Generators that have no deep feature in the loaded data (registered after the data was built) keep running their deep
 * pass from the catch-all decorator ({@link #runUncovered}), and so do mod dimensions, whose chunk generators do not run biome
 * features.
 */
public final class DeepLayer1710 {
    /** One registered legacy generator, with the id of its deep feature. */
    public record Generator(IWorldGenerator generator, int weight, ReentrantLock lock, String id) {
        String className() {
            return generator.getClass().getName();
        }
    }

    static final long DEEP_SEED = 0x5DEECE66DL;
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/WorldGen");

    private static final List<Generator> GENERATORS = new ArrayList<>();
    private static final Map<String, Generator> BY_ID = new ConcurrentHashMap<>();
    /** Ids of generators whose deep feature is part of the generated data (so the catch-all leaves them out). */
    private static final Set<String> FEATURED = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();
    private static final Set<String> UNKNOWN = ConcurrentHashMap.newKeySet();
    private static boolean contributing;

    private DeepLayer1710() {}

    /** Called for every {@code GameRegistry.registerWorldGenerator}: gives the generator its deep feature id. */
    public static synchronized Generator register(IWorldGenerator generator, int weight, ReentrantLock lock) {
        // FML keeps its generators in a set: registering one again adds nothing (ChromatiCraft registers DataTowerGenerator twice)
        for (Generator known : GENERATORS) if (known.generator == generator) return known;
        String id = uniqueId(namespace() + ":legacy_worldgen_deep/" + path(generator.getClass().getName()));
        Generator g = new Generator(generator, weight, lock, id);
        GENERATORS.add(g);
        BY_ID.put(id, g);
        if (!contributing) {
            contributing = true;
            GeneratedPack.addContributor(DeepLayer1710::contribute);
        }
        return g;
    }

    /** The registered generators (in registration order), for a per-generator registration of the normal pass. */
    public static synchronized List<Generator> generators() {
        return List.copyOf(GENERATORS);
    }

    /** The deep feature id of a generator, or null when it is not registered. */
    public static synchronized String featureId(IWorldGenerator generator) {
        for (Generator g : GENERATORS) if (g.generator == generator) return g.id;
        return null;
    }

    /** Whether the generator's deep pass runs at all (config). */
    public static boolean allowed(Generator g) {
        return LegacyConfig.deepLayerEnabled() && LegacyConfig.deepLayerAllows(g.className());
    }

    // ------------------------------------------------------------------ data

    /** Configured feature, placed feature and biome modifier per generator whose deep pass the config allows. */
    static void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.SERVER_DATA) return;
        for (Generator g : generators()) {
            if (!allowed(g)) {
                FEATURED.remove(g.id);
                continue;
            }
            int colon = g.id.indexOf(':');
            String ns = g.id.substring(0, colon);
            String path = g.id.substring(colon + 1);
            put(out, "data/" + ns + "/worldgen/configured_feature/" + path + ".json",
                    "{\"type\":\"legacyconnector:legacy_worldgen_deep\",\"config\":{\"generator\":\"" + g.id + "\"}}");
            // legacy y=0 at the dimension's bottom; the window ends at modern y=0
            put(out, "data/" + ns + "/worldgen/placed_feature/" + path + ".json",
                    "{\"feature\":\"" + g.id + "\",\"placement\":[{\"type\":\"minecraft:height_range\",\"height\":{\"above_bottom\":0}}]}");
            put(out, "data/" + ns + "/neoforge/biome_modifier/" + path + ".json",
                    "{\"type\":\"neoforge:add_features\",\"biomes\":{\"type\":\"neoforge:any\"},\"features\":\"" + g.id + "\",\"step\":\"underground_ores\"}");
            FEATURED.add(g.id);
        }
    }

    private static void put(Map<String, Supplier<byte[]>> out, String path, String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        out.put(path, () -> bytes);
    }

    // ------------------------------------------------------------------ running

    /** Runs the deep pass of the generator {@code id} in one chunk, legacy y=0 at {@code bottomY} (the feature's placed y). */
    static boolean place(WorldGenLevel level, String id, int chunkX, int chunkZ, int bottomY) {
        Generator g = BY_ID.get(id);
        if (g == null) {
            if (UNKNOWN.add(id)) LOG.warn("Deep layer feature for {} has no registered legacy world generator (stale data?): skipped", id);
            return false;
        }
        int shift = Math.max(bottomY, level.getMinBuildHeight());
        if (shift >= 0 || !allowed(g)) return false;
        World world = World.mcc$wrap(level);
        Random random = new Random();
        long chunkSeed = chunkSeed(level.getSeed(), chunkX, chunkZ);
        World.mcc$enterDeepPass(shift);
        try {
            run(g, random, chunkSeed, chunkX, chunkZ, world);
        } finally {
            World.mcc$leaveDeepPass();
        }
        return true;
    }

    /**
     * The catch-all deep pass: generators without a deep feature in the loaded data (all of them in a mod dimension),
     * legacy y=0 at the dimension's bottom.
     */
    static void runUncovered(WorldGenLevel level, World world, int chunkX, int chunkZ, long chunkSeed, boolean modDimension) {
        int bottom = level.getMinBuildHeight();
        if (bottom >= 0 || !LegacyConfig.deepLayerEnabled()) return;
        List<Generator> gens = generators();
        Random random = new Random();
        boolean entered = false;
        try {
            for (Generator g : gens) {
                if (!modDimension && FEATURED.contains(g.id) || !allowed(g)) continue;
                if (!entered) {
                    World.mcc$enterDeepPass(bottom);
                    entered = true;
                }
                run(g, random, chunkSeed, chunkX, chunkZ, world);
            }
        } finally {
            if (entered) World.mcc$leaveDeepPass();
        }
    }

    /** One generator's deep pass with the seeding the catch-all used (FML's chunk seed ^ {@link #DEEP_SEED}). */
    private static void run(Generator g, Random random, long chunkSeed, int chunkX, int chunkZ, World world) {
        random.setSeed(chunkSeed ^ DEEP_SEED);
        try {
            LegacyModLocks.generation(g.lock, () -> g.generator.generate(random, chunkX, chunkZ, world, WorldGenBridge.dummyProvider(), WorldGenBridge.dummyProvider()));
        } catch (RuntimeException | LinkageError ex) {
            if (FAILED.add(g.className())) {
                LOG.warn("Legacy world generator {} failed in the deep layer (it keeps running above y=0): {}", g.className(), ex.toString());
            }
        }
    }

    /** FML's per-chunk seed ({@code GameRegistry.generateWorld}). */
    static long chunkSeed(long worldSeed, int chunkX, int chunkZ) {
        Random fmlRandom = new Random(worldSeed);
        long xSeed = fmlRandom.nextLong() >> 2 + 1L;
        long zSeed = fmlRandom.nextLong() >> 2 + 1L;
        return (xSeed * chunkX + zSeed * chunkZ) ^ worldSeed;
    }

    // ------------------------------------------------------------------ ids

    private static String namespace() {
        String modId = null;
        try {
            var active = org.kylepitcock.legacyconnector.v1_7_10.LegacyModContainer1710.active();
            if (active != null) modId = active.getModId();
        } catch (RuntimeException | LinkageError ignored) {
            // no active mod: the layer's own namespace
        }
        String ns = modId == null ? "" : sanitize(modId, false);
        return ns.isEmpty() ? "legacyconnector" : ns;
    }

    private static String path(String className) {
        String p = sanitize(className, true);
        return p.isEmpty() ? "generator" : p;
    }

    private static String sanitize(String s, boolean path) {
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toLowerCase(Locale.ROOT).toCharArray()) {
            boolean ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.';
            b.append(ok ? c : '_');
        }
        return b.toString();
    }

    /** A second instance of the same generator class (or a case clash) gets a numbered id. */
    private static String uniqueId(String id) {
        if (!BY_ID.containsKey(id)) return id;
        for (int i = 2; ; i++) if (!BY_ID.containsKey(id + "_" + i)) return id + "_" + i;
    }
}
