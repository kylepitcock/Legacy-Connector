package org.kylepitcock.legacyconnector.core.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.kylepitcock.legacyconnector.core.LegacyLifecycle;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy structure generators as modern structures. Each {@link LegacyStructureSource} gets three generated data files, under the
 * legacy mod's namespace ({@code <mod>:legacy/<structure name>}, stable across sessions so saved starts load back):
 * <ul>
 * <li>{@code worldgen/structure}: a {@link LegacyStructure} in the {@code top_layer_modification} step (1.7.10 ran mod generators after
 * the chunk was decorated), no terrain adaptation (legacy pieces find their ground themselves);</li>
 * <li>{@code worldgen/structure_set}: random spread with spacing 1 and separation 0: every chunk is a candidate and the legacy
 * {@code canSpawnStructureAtCoords} (with the mod's own spacing, chance and config) decides, so starts are exactly as frequent and
 * where they were in 1.7.10 for the same seed;</li>
 * <li>{@code tags/worldgen/biome/has_structure/legacy/...}: {@code #minecraft:is_overworld} plus the generator's legacy spawn biomes
 * mapped to modern ones (a pack can edit the tag like any structure's).</li>
 * </ul>
 * <p>A legacy structure was only built where the mod called its generator (its world generator checks the dimension and the mod's
 * config). That stays so: a piece is placed in a chunk only when the mod asked for its structures in that chunk
 * ({@link #activate}, from the legacy {@code generateStructuresInChunk}, which no longer places anything itself). Both happen in the
 * chunk's decoration on the same thread, in either order: a piece the game places first waits until the mod's call.
 */
public final class LegacyStructureRegistry {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Structures");
    private static final List<Supplier<Collection<? extends LegacyStructureSource>>> PROVIDERS = new CopyOnWriteArrayList<>();
    private static final Map<String, LegacyStructureSource> SOURCES = new ConcurrentHashMap<>();
    private static volatile boolean computed;
    private static boolean registered;
    private static boolean announced;

    private LegacyStructureRegistry() {}

    /** Whether legacy structure generators become modern structures ({@code -Dlegacyconnector.modernStructures=false} keeps 1.7.10 placement). */
    public static boolean enabled() {
        return !"false".equalsIgnoreCase(System.getProperty("legacyconnector.modernStructures", "true"));
    }

    /**
     * Registers the structure and piece types and the generated data, once, while registries are open (from the first legacy structure
     * generator constructed during loading). Without it no legacy structure is made modern.
     */
    public static synchronized void ensureRegistered() {
        if (registered || !enabled() || !LegacyLifecycle.isRegistrationOpen()) return;
        registered = true;
        try {
            StructureType<LegacyStructure> type = () -> LegacyStructure.CODEC;
            LegacyRegistrar.register(BuiltInRegistries.STRUCTURE_TYPE, ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_structure"), type);
            LegacyStructure.TYPE = type;
            StructurePieceType.ContextlessType piece = LegacyStructurePiece::new;
            LegacyRegistrar.register(BuiltInRegistries.STRUCTURE_PIECE, ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_piece"), piece);
            LegacyStructurePiece.TYPE = piece;
        } catch (RuntimeException e) {
            LOG.warn("Could not register the legacy structure types; legacy structures keep their 1.7.10 placement: {}", e.toString());
            LegacyStructure.TYPE = null;
            return;
        }
        GeneratedPack.addContributor(LegacyStructureRegistry::contribute);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> clearLevels());
    }

    /** Adds the structure generators of a legacy version (asked once legacy loading is over). */
    public static void addProvider(Supplier<Collection<? extends LegacyStructureSource>> provider) {
        PROVIDERS.add(provider);
    }

    private static void compute() {
        if (computed || LegacyStructure.TYPE == null || LegacyStructurePiece.TYPE == null) return;
        synchronized (LegacyStructureRegistry.class) {
            if (computed) return;
            for (var provider : PROVIDERS) {
                try {
                    for (LegacyStructureSource s : provider.get()) SOURCES.putIfAbsent(s.key(), s);
                } catch (RuntimeException | LinkageError e) {
                    LOG.warn("Listing legacy structure generators failed: {}", e.toString());
                }
            }
            // generators are constructed while mods load: the list is complete once loading is over
            if (!LegacyLifecycle.isRegistrationOpen()) computed = true;
        }
    }

    /** All legacy structures made modern. */
    public static Collection<LegacyStructureSource> sources() {
        compute();
        return SOURCES.values();
    }

    public static LegacyStructureSource source(String key) {
        compute();
        return key == null ? null : SOURCES.get(key);
    }

    // ------------------------------------------------------------------ generated data

    private static void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.SERVER_DATA) return;
        List<String> ids = new ArrayList<>();
        for (LegacyStructureSource s : sources()) {
            ResourceLocation id = s.id();
            String ns = id.getNamespace(), path = id.getPath();
            String tag = ns + ":has_structure/" + path;

            JsonObject structure = new JsonObject();
            structure.addProperty("type", "legacyconnector:legacy_structure");
            structure.addProperty("generator", s.key());
            structure.addProperty("biomes", "#" + tag);
            structure.add("spawn_overrides", new JsonObject());
            structure.addProperty("step", "top_layer_modification");
            structure.addProperty("terrain_adaptation", "none");
            put(out, "data/" + ns + "/worldgen/structure/" + path + ".json", structure);

            JsonObject set = new JsonObject();
            JsonArray entries = new JsonArray();
            JsonObject entry = new JsonObject();
            entry.addProperty("structure", id.toString());
            entry.addProperty("weight", 1);
            entries.add(entry);
            set.add("structures", entries);
            JsonObject placement = new JsonObject();
            placement.addProperty("type", "minecraft:random_spread");
            placement.addProperty("spacing", 1);
            placement.addProperty("separation", 0);
            placement.addProperty("salt", Math.floorMod(id.toString().hashCode(), Integer.MAX_VALUE));
            int villages = villageDistance();
            if (villages > 0) {
                JsonObject zone = new JsonObject();
                zone.addProperty("other_set", "minecraft:villages");
                zone.addProperty("chunk_count", villages);
                placement.add("exclusion_zone", zone);
            }
            set.add("placement", placement);
            put(out, "data/" + ns + "/worldgen/structure_set/" + path + ".json", set);

            JsonObject biomes = new JsonObject();
            JsonArray values = new JsonArray();
            List<ResourceLocation> listed = s.biomes();
            // the legacy canSpawnStructureAtCoords checks the legacy biomes itself (through the same modern-to-legacy biome mapping
            // the legacy world uses): the tag only names the dimension, plus the listed biomes (legacy mod biomes may not be tagged
            // as overworld), so no start the legacy check accepts is lost. A pack can narrow the tag.
            values.add(optional("#minecraft:is_overworld"));
            if (listed != null) for (ResourceLocation b : listed) values.add(optional(b.toString()));
            biomes.add("values", values);
            put(out, "data/" + ns + "/tags/worldgen/biome/has_structure/" + path + ".json", biomes);
            ids.add(id.toString());
        }
        if (!ids.isEmpty() && !announced) {
            announced = true;
            LOG.info("{} legacy structure generator(s) are modern structures: {}", ids.size(), String.join(", ", ids));
        }
    }

    /**
     * Chunks kept between a legacy structure's start and a village placement chunk ({@code -Dlegacyconnector.structureVillageDistance}).
     * Off by default: the exclusion zone also removes starts near village chunks where no village generated, so legacy structures
     * would become rarer than in 1.7.10; actual overlaps are handled by {@link InPlaceStructureGuard} instead.
     */
    private static int villageDistance() {
        return Math.max(0, Math.min(16, Integer.getInteger("legacyconnector.structureVillageDistance", 0)));
    }

    private static JsonObject optional(String id) {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("required", false);
        return o;
    }

    private static void put(Map<String, Supplier<byte[]>> out, String path, JsonObject json) {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        out.put(path, () -> bytes);
    }

    // ------------------------------------------------------------------ levels

    private static final Map<ChunkGenerator, ServerLevel> LEVELS = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<ServerLevel, Set<ResourceLocation>> SETS = Collections.synchronizedMap(new IdentityHashMap<>());

    private static void clearLevels() {
        LEVELS.clear();
        SETS.clear();
        RELAYOUT.clear();
    }

    /** Our own chunk generators (legacy mod dimensions) run the legacy generators the 1.7.10 way: no modern legacy structures there. */
    private static boolean legacyGenerator(ChunkGenerator generator) {
        return generator.getClass().getName().startsWith("org.kylepitcock.legacyconnector.");
    }

    /** The server level a chunk generator belongs to, or null (another mod's detached generator, a legacy mod dimension). */
    static ServerLevel levelOf(ChunkGenerator generator) {
        if (generator == null || legacyGenerator(generator)) return null;
        ServerLevel known = LEVELS.get(generator);
        if (known != null) return known;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        try {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getChunkSource().getGenerator() == generator) {
                    LEVELS.put(generator, level);
                    return level;
                }
            }
        } catch (RuntimeException e) {
            // levels still being created
        }
        return null;
    }

    /** Whether the modern structure {@code id} can generate in a level (its set is one of the level's possible sets). */
    public static boolean generatesIn(ServerLevel level, ResourceLocation id) {
        if (level == null || id == null) return false;
        Set<ResourceLocation> sets = SETS.get(level);
        if (sets == null) {
            Set<ResourceLocation> found = new HashSet<>();
            try {
                var source = level.getChunkSource();
                if (!legacyGenerator(source.getGenerator())) {
                    for (var holder : source.getGeneratorState().possibleStructureSets()) holder.unwrapKey().ifPresent(k -> found.add(k.location()));
                }
            } catch (RuntimeException e) {
                return false;
            }
            SETS.put(level, found);
            sets = found;
        }
        return sets.contains(id);
    }

    // ------------------------------------------------------------------ placement, gated by the mod's own call

    private record Pending(LegacyStructurePiece piece, BoundingBox box, long seed) {}

    /** What the decoration of the chunk this thread decorates has seen: one region at a time, so nothing outlives it. */
    private static final class Decoration {
        final WeakReference<WorldGenLevel> level;
        final Set<String> active = new HashSet<>();
        final List<Pending> pending = new ArrayList<>();

        Decoration(WorldGenLevel level) {
            this.level = new WeakReference<>(level);
        }
    }

    private static final ThreadLocal<Decoration> DECORATION = new ThreadLocal<>();

    private static Decoration decoration(WorldGenLevel level) {
        Decoration d = DECORATION.get();
        if (d == null || d.level.get() != level) {
            d = new Decoration(level);
            DECORATION.set(d);
        }
        return d;
    }

    /** The game places a piece in a chunk: now if the mod asked for its structures in this chunk, else when it does. */
    static void place(LegacyStructurePiece piece, WorldGenLevel level, BoundingBox box, long seed) {
        Decoration d = decoration(level);
        if (d.active.contains(piece.generator())) piece.placeNow(level, box, seed);
        else if (d.pending.size() < 4096) d.pending.add(new Pending(piece, box, seed));
    }

    /**
     * The mod asked for the structures of generator {@code key} in the chunk being decorated (its legacy {@code generateStructuresInChunk}):
     * pieces the game already handed over are placed now, later ones at once.
     */
    public static void activate(WorldGenLevel level, String key) {
        Decoration d = decoration(level);
        if (!d.active.add(key)) return;
        List<Pending> ready = new ArrayList<>();
        d.pending.removeIf(p -> {
            if (!p.piece().generator().equals(key)) return false;
            ready.add(p);
            return true;
        });
        for (Pending p : ready) p.piece().placeNow(level, p.box(), p.seed());
    }

    // ------------------------------------------------------------------ saved pieces without legacy NBT

    private static final Map<String, List<LegacyStructureSource.Part>> RELAYOUT = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<LegacyStructureSource.Part>> eldest) {
                    return size() > 64;
                }
            });

    /** A saved piece's legacy object, laid out again from the seed (starts are deterministic). */
    static Object relayout(LegacyStructureSource source, WorldGenLevel level, int startX, int startZ, int index) {
        String k = source.key() + "@" + startX + "," + startZ;
        List<LegacyStructureSource.Part> parts = RELAYOUT.get(k);
        if (parts == null) {
            try {
                parts = source.start(level.getLevel(), level.getSeed(), startX, startZ);
            } catch (RuntimeException | LinkageError e) {
                failed(source.key(), "laying out a saved start again", e);
                parts = null;
            }
            if (parts == null) parts = List.of();
            RELAYOUT.put(k, parts);
        }
        return index >= 0 && index < parts.size() ? parts.get(index).handle() : null;
    }

    // ------------------------------------------------------------------ logging

    private static final Map<String, AtomicInteger> FAILURES = new ConcurrentHashMap<>();

    /** Logs a failure of a legacy structure generator, throttled per generator. */
    public static void failed(String key, String what, Throwable e) {
        int n = FAILURES.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        if (n <= 3) LOG.warn("Legacy structure {} failed {} ({} so far)", key, what, n, e);
        else if (n % 500 == 0) LOG.warn("Legacy structure {} failed {} times so far (last: {})", key, n, e.toString());
    }
}
