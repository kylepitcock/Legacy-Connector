package org.kylepitcock.legacyconnector.core.worldgen;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps legacy generators that build in place (huts, shrines, towers, dungeons written block by block from an {@code IWorldGenerator})
 * out of modern structures, without knowing which generator is a "structure" generator.
 *
 * <p>Only in a chunk where a modern structure's piece reaches into the decorated area (the region's centre chunk and its neighbours)
 * is anything done; elsewhere a generator runs untouched, with its own per-chunk chance. There, each legacy generator call is
 * journaled: every write keeps the block it replaced. A write into a modern structure piece's box is refused, unless it only turns
 * natural ground (stone, dirt, sand, gravel...) into another non-air block (ores in the rock under a house). After the call:
 * <ul>
 * <li>when the call built something ({@value #BUILD_MIN} or more "build" writes: a block into air, water or a replaceable plant, a
 * block removed, or a block entity) and at least {@value #REFUSED_MIN} of them were refused, it was a structure crossing a modern
 * one: the whole call is undone (blocks, block entities, entities it added, writes it deferred to unloaded chunks), as if the
 * generator had skipped this chunk;</li>
 * <li>otherwise the call stays, minus the few refused blocks (a flower or some leaves inside a house's box).</li>
 * </ul>
 * Pieces of legacy structures made modern ({@link LegacyStructurePiece}) do not count as modern structures here (legacy generators
 * overlapped each other in 1.7.10 too), but their placement runs through the same check against the other structures.
 * Turned off with {@code -Dlegacyconnector.inPlaceGuard=false}.
 */
public final class InPlaceStructureGuard {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Structures");
    static final int BUILD_MIN = Integer.getInteger("legacyconnector.inPlaceGuard.buildMin", 24);
    static final int REFUSED_MIN = Integer.getInteger("legacyconnector.inPlaceGuard.refusedMin", 4);
    private static final boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty("legacyconnector.inPlaceGuard", "true"));
    private static final AtomicInteger UNDONE = new AtomicInteger();

    private InPlaceStructureGuard() {}

    private record Old(BlockState state, CompoundTag blockEntity) {}

    private static final class Journal {
        final WorldGenRegion region;
        final List<BoundingBox> boxes;
        final Map<BlockPos, Old> old = new LinkedHashMap<>();
        final List<Runnable> deferred = new ArrayList<>();
        final int[] entities = new int[9];
        int builds;
        int refusedBuilds;

        Journal(WorldGenRegion region, List<BoundingBox> boxes) {
            this.region = region;
            this.boxes = boxes;
        }
    }

    private static final ThreadLocal<Journal> JOURNAL = new ThreadLocal<>();

    /** The boxes of the modern structure pieces in the decorated area of the region this thread works on (computed once per region). */
    private static final ThreadLocal<Object[]> BOXES = new ThreadLocal<>();

    /**
     * Runs one legacy generator call for the chunk being decorated under the guard; {@code owner} names it in the log. Never throws
     * more than the body does.
     */
    public static void run(WorldGenLevel level, Object owner, Runnable body) {
        guarded(level, owner, () -> {
            body.run();
            return true;
        });
    }

    /** As {@link #run}, for a body with a result (false when the call was undone). */
    public static boolean guarded(WorldGenLevel level, Object owner, BooleanSupplier body) {
        if (!ENABLED || !(level instanceof WorldGenRegion region)) return body.getAsBoolean();
        List<BoundingBox> boxes = boxes(region);
        Journal outer = JOURNAL.get();
        if (boxes.isEmpty()) {
            if (outer == null) return body.getAsBoolean();
            // nested call (a modern legacy piece placed from inside a guarded generator): not part of the outer call
            JOURNAL.remove();
            try {
                return body.getAsBoolean();
            } finally {
                JOURNAL.set(outer);
            }
        }
        Journal j = new Journal(region, boxes);
        countEntities(j, false);
        JOURNAL.set(j);
        boolean result;
        try {
            result = body.getAsBoolean();
        } catch (RuntimeException | Error e) {
            JOURNAL.set(outer);
            finish(j, owner);
            throw e;
        }
        if (outer == null) JOURNAL.remove();
        else JOURNAL.set(outer);
        return finish(j, owner) && result;
    }

    /** Runs code (a legacy structure piece the game places) outside any journal of the thread. */
    public static boolean unguarded(BooleanSupplier body) {
        Journal outer = JOURNAL.get();
        if (outer == null) return body.getAsBoolean();
        JOURNAL.remove();
        try {
            return body.getAsBoolean();
        } finally {
            JOURNAL.set(outer);
        }
    }

    /** Called by the legacy world before it writes a block: false refuses the write. */
    public static boolean allowWrite(LevelAccessor level, BlockPos pos, BlockState next) {
        Journal j = JOURNAL.get();
        if (j == null || j.region != level || next == null) return true;
        try {
            BlockState prev = j.region.getBlockState(pos);
            boolean build = isBuild(prev, next);
            if (inside(j.boxes, pos) && !(isGround(prev) && !next.isAir())) {
                if (build) {
                    j.builds++;
                    j.refusedBuilds++;
                }
                return false;
            }
            if (build) j.builds++;
            if (!j.old.containsKey(pos)) {
                CompoundTag be = null;
                if (prev.hasBlockEntity()) {
                    try {
                        be = j.region.getChunk(pos).getBlockEntityNbtForSaving(pos, j.region.registryAccess());
                    } catch (RuntimeException e) {
                        be = null;
                    }
                }
                j.old.put(pos.immutable(), new Old(prev, be));
            }
        } catch (RuntimeException e) {
            // a position the region cannot read: let the write decide
        }
        return true;
    }

    /** A write past the region (placed when that chunk generates): held until the guarded call is kept. False: run it now. */
    public static boolean holdDeferred(LevelAccessor level, Runnable write) {
        Journal j = JOURNAL.get();
        if (j == null || j.region != level) return false;
        j.deferred.add(write);
        return true;
    }

    private static boolean isBuild(BlockState prev, BlockState next) {
        if (next.hasBlockEntity()) return true;
        if (next.isAir()) return !prev.isAir();
        return prev.isAir() || prev.canBeReplaced() || !prev.getFluidState().isEmpty();
    }

    /** Natural ground (writes that keep it solid are ores and the like). Block tags are bound while chunks generate. */
    private static boolean isGround(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.BASE_STONE_NETHER) || s.is(BlockTags.DIRT) || s.is(BlockTags.SAND)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.END_STONE) || s.is(BlockTags.TERRACOTTA) || s.is(Blocks.SANDSTONE) || s.is(Blocks.CLAY);
    }

    private static boolean inside(List<BoundingBox> boxes, BlockPos pos) {
        for (BoundingBox b : boxes) if (b.isInside(pos)) return true;
        return false;
    }

    /** Keeps or undoes a finished call; true when kept. */
    private static boolean finish(Journal j, Object owner) {
        boolean undo = j.refusedBuilds >= REFUSED_MIN && j.builds >= BUILD_MIN;
        if (!undo) {
            for (Runnable r : j.deferred) {
                try {
                    r.run();
                } catch (RuntimeException e) {
                    // a deferred write that cannot be queued is lost, as it would have been
                }
            }
            return true;
        }
        List<Map.Entry<BlockPos, Old>> entries = new ArrayList<>(j.old.entrySet());
        Collections.reverse(entries);
        for (var e : entries) {
            try {
                BlockPos p = e.getKey();
                Old o = e.getValue();
                j.region.setBlock(p, o.state(), 2 | 16);
                if (o.blockEntity() != null) j.region.getChunk(p).setBlockEntityNbt(o.blockEntity());
            } catch (RuntimeException ex) {
                // keep undoing the rest
            }
        }
        countEntities(j, true);
        int n = UNDONE.incrementAndGet();
        if (n <= 10 || n % 200 == 0) {
            ChunkPos c = j.region.getCenter();
            LOG.info("Legacy generation by {} in chunk {},{} built into a modern structure ({} blocks, {} refused): left out ({} so far)",
                    owner instanceof String named ? named : owner instanceof Class<?> k ? k.getName() : owner == null ? "?" : owner.getClass().getName(), c.x, c.z, j.builds, j.refusedBuilds, n);
        }
        return false;
    }

    /** Before a call: the entity count of the 3x3 chunks; on undo: drops the entities the call added. */
    private static void countEntities(Journal j, boolean undo) {
        ChunkPos c = j.region.getCenter();
        int i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++, i++) {
                try {
                    ChunkAccess chunk = j.region.getChunk(c.x + dx, c.z + dz);
                    if (!(chunk instanceof ProtoChunk proto)) continue;
                    List<CompoundTag> list = proto.getEntities();
                    if (!undo) j.entities[i] = list.size();
                    else while (list.size() > j.entities[i]) list.remove(list.size() - 1);
                } catch (RuntimeException e) {
                    // not reachable: nothing to count
                }
            }
        }
    }

    /** The modern (non-legacy) structure piece boxes reaching into the region's centre chunk and its neighbours. */
    private static List<BoundingBox> boxes(WorldGenRegion region) {
        Object[] cached = BOXES.get();
        if (cached != null && ((WeakReference<?>) cached[0]).get() == region) {
            @SuppressWarnings("unchecked")
            List<BoundingBox> list = (List<BoundingBox>) cached[1];
            return list;
        }
        List<BoundingBox> out = new ArrayList<>();
        try {
            ChunkPos c = region.getCenter();
            BoundingBox area = new BoundingBox((c.x - 1) << 4, region.getMinBuildHeight(), (c.z - 1) << 4, ((c.x + 1) << 4) + 15,
                    region.getMaxBuildHeight(), ((c.z + 1) << 4) + 15);
            Set<StructureStart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ChunkAccess chunk = region.getChunk(c.x + dx, c.z + dz);
                    for (var ref : chunk.getAllReferences().entrySet()) {
                        for (long packed : ref.getValue()) {
                            int sx = ChunkPos.getX(packed), sz = ChunkPos.getZ(packed);
                            if (!region.hasChunk(sx, sz)) continue;
                            ChunkAccess startChunk = region.getChunk(sx, sz, ChunkStatus.STRUCTURE_STARTS, false);
                            if (startChunk == null) continue;
                            StructureStart start = startChunk.getStartForStructure(ref.getKey());
                            if (start == null || !start.isValid() || !seen.add(start)) continue;
                            for (StructurePiece piece : start.getPieces()) {
                                if (piece instanceof LegacyStructurePiece) continue;
                                if (piece.getBoundingBox().intersects(area)) out.add(piece.getBoundingBox());
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            // a region that cannot be inspected: no guard
        }
        BOXES.set(new Object[] {new WeakReference<>(region), out});
        return out;
    }
}
