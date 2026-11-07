package org.kylepitcock.legacyconnector.core.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import org.kylepitcock.legacyconnector.core.LegacyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes what legacy world generators build in place (ChromatiCraft's pylons and dungeons, Thaumcraft's mounds and obelisks, Witchery's
 * huts: written block by block from an {@code IWorldGenerator}) locatable with {@code /locate structure}, without changing generation.
 *
 * <p>Every legacy generator with a placed feature of its own ({@link LegacyGenerators}) gets a modern structure
 * {@code <mod>:legacy/<name>} ({@link LegacyGeneratorStructure}; the name is the generator's class without {@code _generator}, e.g.
 * {@code chromaticraft:legacy/pylon}; a mod's single world generator is {@code <mod>:legacy/structures}), with a structure set and the
 * biome tag {@code <mod>:has_structure/legacy/<name>} (the overworld, the Nether and the End). The game never lays out a start of it.
 * Instead, each run of the generator in a chunk's decoration is observed: the writes the legacy world makes ({@link #observeWrite}, from
 * {@link InPlaceStructureGuard#allowWrite}). When the run built a structure ({@value #BUILD_MIN_DEFAULT} or more blocks into air, water
 * or plants, of which at least {@value #MADE_MIN_DEFAULT} are not natural: not ground, ores, logs, leaves, plants or fluids, or block
 * entities), a start with one {@link LegacyMarkerPiece} covering the built area is added to the chunk at its centre and the chunk's
 * structure data is refreshed, so the start is saved with the chunk and {@code /locate} finds it like any modern structure. Trees and
 * plants are natural, ores replace ground: neither counts. A generator that "builds" in more than a quarter of the chunks it ran in is
 * no structure generator; it is no longer recorded (logged once).
 *
 * <p>Limits: only structures in chunks that have generated are found (locate does not create them in new chunks, see
 * {@link LegacyGeneratorStructure#findGenerationPoint}); one structure per generator (a generator building several kinds is one id);
 * a call undone by {@link InPlaceStructureGuard} is not recorded; writes a generator defers past its region are not part of the box.
 * Switch: {@code structures.locatable} (when false, the structures stay defined without biomes, so recorded starts still load).
 */
public final class LegacyGeneratorStructures {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Structures");
    static final int BUILD_MIN_DEFAULT = 24;
    static final int MADE_MIN_DEFAULT = 8;
    private static final int BUILD_MIN = Integer.getInteger("legacyconnector.locatable.buildMin", BUILD_MIN_DEFAULT);
    private static final int MADE_MIN = Integer.getInteger("legacyconnector.locatable.madeMin", MADE_MIN_DEFAULT);
    /** Runs before a generator's build share is judged, and the share above which it is no structure generator. */
    private static final int SHARE_MIN_CALLS = 256;
    private static final double SHARE_MAX = 0.25;

    /** Feature id of the generator ({@code <mod>:legacy_worldgen/...}) -> its structure id; assigned once per session (stable ids). */
    private static final Map<ResourceLocation, ResourceLocation> STRUCTURES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Stats> STATS = new ConcurrentHashMap<>();
    private static final List<Predicate<BlockState>> NATURAL = new CopyOnWriteArrayList<>();
    private static final Map<Block, Boolean> NATURAL_CACHE = new ConcurrentHashMap<>();
    private static final AtomicInteger MARKS = new AtomicInteger();
    private static volatile boolean announced;

    private LegacyGeneratorStructures() {}

    /** {@code structures.locatable}: legacy generator structures are recorded and locatable. */
    public static boolean enabled() {
        return LegacyConfig.structuresLocatable() && LegacyGeneratorStructure.TYPE != null && LegacyMarkerPiece.TYPE != null;
    }

    /** A version bridge's test for natural legacy blocks (its logs, leaves, plants, fluids), which do not make a build a structure. */
    public static void addNaturalBlocks(Predicate<BlockState> test) {
        NATURAL.add(test);
        NATURAL_CACHE.clear();
    }

    private static final class Stats {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger marks = new AtomicInteger();
        volatile boolean off;
    }

    // ------------------------------------------------------------------ generated data

    /** The structure of a generator's feature id, or null (none: not observable, or registered after the data was built). */
    static ResourceLocation structureOf(ResourceLocation feature) {
        return feature == null ? null : STRUCTURES.get(feature);
    }

    /** Writes structure, structure set and biome tag of every observable generator (from {@link LegacyGenerators#contribute}). */
    static void contribute(List<ResourceLocation> features, Map<String, Supplier<byte[]>> out) {
        if (LegacyGeneratorStructure.TYPE == null) return;
        boolean on = LegacyConfig.structuresLocatable();
        List<String> ids = new ArrayList<>();
        synchronized (STRUCTURES) {
            for (ResourceLocation feature : features) {
                ResourceLocation id = STRUCTURES.computeIfAbsent(feature, LegacyGeneratorStructures::newId);
                write(id, feature, on, out);
                ids.add(id.toString());
            }
        }
        if (on && !ids.isEmpty() && !announced) {
            announced = true;
            LOG.info("{} legacy world generator(s) are locatable structures (recorded where they build): {}", ids.size(), String.join(", ", ids));
        }
    }

    private static ResourceLocation newId(ResourceLocation feature) {
        String ns = feature.getNamespace();
        String base = feature.getPath();
        base = base.substring(base.lastIndexOf('/') + 1);
        String suffix = "";
        var m = java.util.regex.Pattern.compile("^(.*?)(_\\d+)?$").matcher(base);
        if (m.matches()) {
            base = m.group(1);
            suffix = m.group(2) == null ? "" : m.group(2);
        }
        String name = base.replaceAll("(_world)?_gen(erator)?$", "").replaceAll("^world_gen(erator)?_", "");
        if (name.startsWith(ns + "_")) name = name.substring(ns.length() + 1);
        if (name.isEmpty() || name.equals(ns) || name.equals("world") || name.equals("gen") || name.equals("generator")) name = "structures";
        String path = "legacy/" + name + suffix;
        ResourceLocation id = ResourceLocation.tryBuild(ns, path);
        if (id == null) id = ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy/generator");
        String p = id.getPath();
        for (int n = 2; taken(id); n++) id = id.withPath(p + "_" + n);
        return id;
    }

    /** An id already used by another generator, or by a legacy MapGenStructure made modern (LegacyStructureRegistry). */
    private static boolean taken(ResourceLocation id) {
        if (STRUCTURES.containsValue(id)) return true;
        for (LegacyStructureSource s : LegacyStructureRegistry.sources()) if (id.equals(s.id())) return true;
        return false;
    }

    private static void write(ResourceLocation id, ResourceLocation feature, boolean on, Map<String, Supplier<byte[]>> out) {
        String ns = id.getNamespace(), path = id.getPath();
        String tag = ns + ":has_structure/" + path;

        JsonObject structure = new JsonObject();
        structure.addProperty("type", "legacyconnector:legacy_generator_structure");
        structure.addProperty("generator", feature.toString());
        structure.addProperty("biomes", "#" + tag);
        structure.add("spawn_overrides", new JsonObject());
        structure.addProperty("step", "top_layer_modification");
        structure.addProperty("terrain_adaptation", "none");
        put(out, "data/" + ns + "/worldgen/structure/" + path + ".json", structure);

        // spacing 1: every chunk can hold a recorded start; the game itself starts none (findGenerationPoint is empty)
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
        JsonArray offset = new JsonArray();
        offset.add(8);
        offset.add(0);
        offset.add(8);
        placement.add("locate_offset", offset); // /locate names the middle of the start's chunk
        set.add("placement", placement);
        put(out, "data/" + ns + "/worldgen/structure_set/" + path + ".json", set);

        // where the generator may build: the vanilla dimensions (legacy generators check the dimension themselves). Empty when switched
        // off: the structure stays defined (recorded starts load) but is in no dimension's structure sets, so locate skips it.
        JsonObject biomes = new JsonObject();
        JsonArray values = new JsonArray();
        if (on) {
            values.add(optional("#minecraft:is_overworld"));
            values.add(optional("#minecraft:is_nether"));
            values.add(optional("#minecraft:is_end"));
        }
        biomes.add("values", values);
        put(out, "data/" + ns + "/tags/worldgen/biome/has_structure/" + path + ".json", biomes);
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

    // ------------------------------------------------------------------ observing one generator run

    /** What one run of a legacy generator in one chunk wrote. */
    public static final class Observation {
        final WorldGenRegion region;
        final ResourceLocation structure;
        final Observation outer;
        int builds;
        int made;
        boolean undone;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        Observation(WorldGenRegion region, ResourceLocation structure, Observation outer) {
            this.region = region;
            this.structure = structure;
            this.outer = outer;
        }

        void record(BlockPos p, BlockState prev, BlockState next) {
            if (!InPlaceStructureGuard.isBuild(prev, next)) return;
            builds++;
            if (next.hasBlockEntity() || !next.isAir() && !natural(next)) made++;
            int x = p.getX(), y = p.getY(), z = p.getZ();
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }
    }

    private static final ThreadLocal<Observation> OBSERVATION = new ThreadLocal<>();

    /**
     * Starts observing a run of the generator whose feature is {@code feature} in the chunk {@code level} decorates; null when there is
     * nothing to record (switched off, no structure, not a generation region, a generator judged no structure generator). Pass the
     * result to {@link #end}.
     */
    public static Observation begin(WorldGenLevel level, ResourceLocation feature) {
        Observation outer = OBSERVATION.get();
        if (!(level instanceof WorldGenRegion region) || !enabled()) return null;
        ResourceLocation id = structureOf(feature);
        if (id == null) return null;
        Stats s = STATS.get(id);
        if (s != null && s.off) return null;
        Observation o = new Observation(region, id, outer);
        OBSERVATION.set(o);
        return o;
    }

    /** Ends an observation: records a start where the run built a structure. Never throws. */
    public static void end(Observation o) {
        if (o == null) return;
        if (o.outer == null) OBSERVATION.remove();
        else OBSERVATION.set(o.outer);
        try {
            Stats s = STATS.computeIfAbsent(o.structure, k -> new Stats());
            int calls = s.calls.incrementAndGet();
            if (o.undone || o.builds < BUILD_MIN || o.made < MADE_MIN || s.off) return;
            int marks = s.marks.incrementAndGet();
            if (calls >= SHARE_MIN_CALLS && marks > calls * SHARE_MAX) {
                s.off = true;
                LOG.info("Legacy world generator structure {} built in {} of {} chunks: not a structure generator, no longer recorded", o.structure, marks, calls);
                return;
            }
            mark(o);
        } catch (RuntimeException | LinkageError e) {
            FailureLog.failed(o.structure, e);
        }
    }

    /** Pauses the thread's observation (a legacy structure piece placed from inside a generator run is not part of it). */
    public static Observation suspend() {
        Observation o = OBSERVATION.get();
        if (o != null) OBSERVATION.remove();
        return o;
    }

    public static void resume(Observation o) {
        if (o != null) OBSERVATION.set(o);
    }

    /** Whether a write into {@code level} on this thread is observed (the guard then reads the block it replaces). */
    static boolean observing(LevelAccessor level) {
        Observation o = OBSERVATION.get();
        return o != null && o.region == level;
    }

    /** A write the legacy world makes (after the guard allowed it). */
    static void observeWrite(LevelAccessor level, BlockPos pos, BlockState prev, BlockState next) {
        Observation o = OBSERVATION.get();
        if (o != null && o.region == level && next != null && prev != null) o.record(pos, prev, next);
    }

    /**
     * A write the legacy world defers past the region (placed when that chunk generates): part of what the run built. The block it
     * replaces is not known yet; a non-air block counts as built into air.
     */
    public static void observeDeferred(LevelAccessor level, BlockPos pos, BlockState next) {
        Observation o = OBSERVATION.get();
        if (o != null && o.region == level && next != null && !next.isAir()) o.record(pos, Blocks.AIR.defaultBlockState(), next);
    }

    /** The guard undid the current call in {@code region}: nothing of it stays, nothing is recorded. */
    static void undone(LevelAccessor region) {
        for (Observation o = OBSERVATION.get(); o != null; o = o.outer) if (o.region == region) o.undone = true;
    }

    /** Natural blocks: what terrain, trees, plants, ores and fluids are made of (they do not make a build a structure). */
    static boolean natural(BlockState s) {
        Block b = s.getBlock();
        Boolean known = NATURAL_CACHE.get(b);
        if (known != null) return known;
        boolean n = computeNatural(s);
        if (NATURAL_CACHE.size() < 100_000) NATURAL_CACHE.put(b, n);
        return n;
    }

    private static boolean computeNatural(BlockState s) {
        if (s.isAir() || !s.getFluidState().isEmpty() && s.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) return true;
        if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.REPLACEABLE)
                || s.is(BlockTags.REPLACEABLE_BY_TREES) || s.is(BlockTags.CROPS) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.BASE_STONE_NETHER)
                || s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.ICE) || s.is(BlockTags.SNOW) || s.is(BlockTags.TERRACOTTA)
                || s.is(BlockTags.CORALS) || s.is(BlockTags.CORAL_BLOCKS) || s.is(BlockTags.CAVE_VINES) || s.is(net.neoforged.neoforge.common.Tags.Blocks.ORES)) {
            return true;
        }
        Block b = s.getBlock();
        if (b instanceof net.minecraft.world.level.block.BushBlock || b instanceof net.minecraft.world.level.block.VineBlock
                || b instanceof net.minecraft.world.level.block.LeavesBlock || b instanceof net.minecraft.world.level.block.GrowingPlantBlock
                || b instanceof net.minecraft.world.level.block.CactusBlock || b instanceof net.minecraft.world.level.block.SugarCaneBlock
                || b instanceof net.minecraft.world.level.block.HugeMushroomBlock || b instanceof net.minecraft.world.level.block.SnowLayerBlock
                || b instanceof net.minecraft.world.level.block.CocoaBlock || b instanceof net.minecraft.world.level.block.BambooStalkBlock) {
            return true;
        }
        if (b == Blocks.GRAVEL || b == Blocks.CLAY || b == Blocks.SANDSTONE || b == Blocks.RED_SANDSTONE || b == Blocks.OBSIDIAN
                || b == Blocks.END_STONE || b == Blocks.BEDROCK || b == Blocks.MAGMA_BLOCK || b == Blocks.GLOWSTONE || b == Blocks.MOSS_BLOCK) {
            return true;
        }
        for (Predicate<BlockState> p : NATURAL) {
            try {
                if (p.test(s)) return true;
            } catch (RuntimeException | LinkageError e) {
                // not decidable: made
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ recording a start

    private static void mark(Observation o) {
        WorldGenRegion region = o.region;
        ServerLevel level = region.getLevel();
        if (!LegacyStructureRegistry.generatesIn(level, o.structure)) return; // no such set here (superflat, a mod dimension)
        Structure structure = region.registryAccess().registry(Registries.STRUCTURE).map(r -> r.get(o.structure)).orElse(null);
        if (structure == null) return;
        BoundingBox box = new BoundingBox(o.minX, o.minY, o.minZ, o.maxX, o.maxY, o.maxZ);
        ChunkPos centre = region.getCenter();
        // the start sits in the chunk at the middle of what was built (legacy generators build at +8, often across chunk borders),
        // within the region's writable area
        int cx = clamp(((o.minX + o.maxX) >> 1) >> 4, centre.x - 1, centre.x + 1);
        int cz = clamp(((o.minZ + o.maxZ) >> 1) >> 4, centre.z - 1, centre.z + 1);
        ChunkPos at = new ChunkPos(cx, cz);
        ChunkAccess chunk = region.getChunk(cx, cz);
        StructureStart existing = chunk.getStartForStructure(structure);
        if (existing != null && existing.isValid()) return; // already recorded there
        StructureStart start = new StructureStart(structure, at, 0, new PiecesContainer(List.of(new LegacyMarkerPiece(box))));
        chunk.setStartForStructure(structure, start);
        long packed = at.toLong();
        for (int x = centre.x - 1; x <= centre.x + 1; x++) {
            for (int z = centre.z - 1; z <= centre.z + 1; z++) {
                if (!box.intersects(x << 4, z << 4, (x << 4) + 15, (z << 4) + 15) || !region.hasChunk(x, z)) continue;
                region.getChunk(x, z).addReferenceForStructure(structure, packed);
            }
        }
        // StructureCheck (what /locate asks) keeps what it read of loaded chunks: refresh this one
        level.onStructureStartsAvailable(chunk);
        LegacyStructureIndex.record(level, o.structure, (o.minX + o.maxX) >> 1, (o.minZ + o.maxZ) >> 1);
        int n = MARKS.incrementAndGet();
        if (n <= 5 || n % 500 == 0) {
            LOG.info("Legacy structure {} recorded at {} {} {} ({} blocks; {} recorded so far)", o.structure, (o.minX + o.maxX) >> 1, o.minY,
                    (o.minZ + o.maxZ) >> 1, o.builds, n);
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Throttled failure log. */
    private static final class FailureLog {
        private static final Set<ResourceLocation> SEEN = ConcurrentHashMap.newKeySet();

        static void failed(ResourceLocation id, Throwable e) {
            if (SEEN.add(id)) LOG.warn("Recording legacy structure {} failed (logged once): {}", id, e.toString());
        }
    }
}
