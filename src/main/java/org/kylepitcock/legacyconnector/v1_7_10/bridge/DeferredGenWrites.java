package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blocks a legacy populator places beyond the chunks its generation region covers. 1.7.10 generated or loaded any chunk a
 * populator wrote to; a modern region only holds the chunk being decorated and its neighbours, and the writes were dropped.
 * Witchery's Torment dimension builds its six 63-block mazes while populating chunk (0, 0), so the far half of every level was
 * missing and the Lord of Torment, spawned at z 36, fell into the void. The writes are kept per chunk and placed once that chunk
 * is loaded as a full chunk (at once, if it already is).
 */
public final class DeferredGenWrites {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/WorldGen");
    /** Writes kept at most, over all levels (a runaway generator must not exhaust the heap). */
    private static final int MAX = 1_000_000;

    /** A block to place, or (state null) a metadata change of the block found there. */
    private record Write(BlockPos pos, BlockState state, int meta, Consumer<BlockEntity> attach) {
        Write(BlockPos pos, BlockState state, int meta) {
            this(pos, state, meta, null);
        }
    }

    /** The block a deferred write will place, per position (what a populator that reads the block back sees). */
    private static final Map<ServerLevel, Map<BlockPos, BlockState>> STATES = new ConcurrentHashMap<>();
    /** Stand-ins for the tile entities of deferred blocks, per position. */
    private static final Map<ServerLevel, Map<BlockPos, Object>> TILES = new ConcurrentHashMap<>();

    private static final Map<ServerLevel, Map<Long, Queue<Write>>> PENDING = new ConcurrentHashMap<>();
    private static final AtomicInteger COUNT = new AtomicInteger();
    private static volatile boolean listening;
    private static volatile boolean warnedFull;

    private DeferredGenWrites() {
    }

    public static void block(ServerLevel level, BlockPos pos, BlockState state) {
        BlockPos key = pos.immutable();
        STATES.computeIfAbsent(level, l -> new ConcurrentHashMap<>()).put(key, state);
        add(level, new Write(key, state, -1));
    }

    /** The stand-in tile entity of a deferred block, or null. */
    public static Object standIn(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Object> m = TILES.get(level);
        return m == null ? null : m.get(pos);
    }

    /** The populator set a tile entity itself: it replaces the stand-in (when a block is waiting there). */
    public static boolean replaceStandIn(ServerLevel level, BlockPos pos, Object tile) {
        Map<BlockPos, Object> m = TILES.get(level);
        if (m == null || !m.containsKey(pos)) return false;
        m.put(pos.immutable(), tile);
        return true;
    }

    /** The block still waiting to be placed at the position, or null. */
    public static BlockState pendingState(ServerLevel level, BlockPos pos) {
        Map<BlockPos, BlockState> m = STATES.get(level);
        return m == null ? null : m.get(pos);
    }

    /**
     * The stand-in tile entity of the deferred block at the position: made by {@code factory} (given the pending state) on first
     * request, and handed to {@code attach} when the block is placed, so what the populator stored in it (chest loot) is not lost.
     * Null when no block is waiting there or the factory made none.
     */
    public static Object tile(ServerLevel level, BlockPos pos, Function<BlockState, Object> factory, Consumer<BlockEntity> attach) {
        BlockState state = pendingState(level, pos);
        if (state == null) return null;
        BlockPos key = pos.immutable();
        Map<BlockPos, Object> tiles = TILES.computeIfAbsent(level, l -> new ConcurrentHashMap<>());
        Object existing = tiles.get(key);
        if (existing != null) return existing;
        Object made = factory.apply(state);
        if (made == null) return null;
        LOG.info("Stand-in tile entity {} made for the deferred block {} at {}", made.getClass().getName(), state.getBlock(), key);
        Object raced = tiles.putIfAbsent(key, made);
        if (raced != null) return raced;
        add(level, new Write(key, null, -1, attach));
        return made;
    }

    public static void meta(ServerLevel level, BlockPos pos, int meta) {
        add(level, new Write(pos.immutable(), null, meta));
    }

    private static void add(ServerLevel level, Write write) {
        if (COUNT.get() >= MAX) {
            if (!warnedFull) {
                warnedFull = true;
                LOG.warn("Too many blocks placed by legacy generators outside their chunks; further ones are dropped");
            }
            return;
        }
        listen();
        long key = ChunkPos.asLong(write.pos.getX() >> 4, write.pos.getZ() >> 4);
        boolean[] created = {false};
        PENDING.computeIfAbsent(level, l -> new ConcurrentHashMap<>()).computeIfAbsent(key, k -> {
            created[0] = true;
            return new ConcurrentLinkedQueue<>();
        }).add(write);
        COUNT.incrementAndGet();
        // the chunk may be loaded already (generated earlier): place the blocks on the server thread, once the populator is done
        if (created[0]) {
            level.getServer().execute(() -> {
                if (level.getChunkSource().getChunkNow(write.pos.getX() >> 4, write.pos.getZ() >> 4) != null) apply(level, key);
            });
        }
    }

    private static synchronized void listen() {
        if (listening) return;
        listening = true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Load e) -> {
            if (!(e.getLevel() instanceof ServerLevel level) || !(e.getChunk() instanceof LevelChunk chunk)) return;
            Map<Long, Queue<Write>> byChunk = PENDING.get(level);
            if (byChunk == null) return;
            long key = chunk.getPos().toLong();
            if (byChunk.containsKey(key)) level.getServer().execute(() -> apply(level, key));
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload e) -> {
            if (e.getLevel() instanceof ServerLevel level) {
                STATES.remove(level);
                TILES.remove(level);
                Map<Long, Queue<Write>> dropped = PENDING.remove(level);
                if (dropped != null) dropped.values().forEach(q -> COUNT.addAndGet(-q.size()));
            }
        });
    }

    /**
     * Send to clients, but no shape updates: those read the neighbouring blocks, and a write on a chunk border waits forever on
     * the neighbouring chunk when it is still being generated (the server thread hung entering a Thaumcraft dimension).
     */
    private static final int WRITE_FLAGS = 2 | 16 | 32;

    private static void apply(ServerLevel level, long key) {
        Map<Long, Queue<Write>> byChunk = PENDING.get(level);
        if (byChunk == null) return;
        Queue<Write> writes = byChunk.remove(key);
        if (writes == null) return;
        COUNT.addAndGet(-writes.size());
        for (Write w : writes) {
            try {
                if (w.attach != null) {
                    BlockEntity be = level.getBlockEntity(w.pos);
                    if (be != null) w.attach.accept(be);
                    Map<BlockPos, Object> tiles = TILES.get(level);
                    if (tiles != null) tiles.remove(w.pos);
                } else if (w.state != null) {
                    Map<BlockPos, BlockState> states = STATES.get(level);
                    if (states != null) states.remove(w.pos, w.state);
                    level.setBlock(w.pos, w.state, WRITE_FLAGS);
                } else {
                    BlockState current = level.getBlockState(w.pos);
                    BlockState next = BlockBridge.withMeta(current, w.meta);
                    if (next != current) level.setBlock(w.pos, next, WRITE_FLAGS);
                }
            } catch (RuntimeException e) {
                LOG.debug("Deferred legacy generation write at {} failed", w.pos, e);
            }
        }
    }
}
