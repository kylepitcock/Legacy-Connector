package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
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
    private record Write(BlockPos pos, BlockState state, int meta) {
    }

    private static final Map<ServerLevel, Map<Long, Queue<Write>>> PENDING = new ConcurrentHashMap<>();
    private static final AtomicInteger COUNT = new AtomicInteger();
    private static volatile boolean listening;
    private static volatile boolean warnedFull;

    private DeferredGenWrites() {
    }

    public static void block(ServerLevel level, BlockPos pos, BlockState state) {
        add(level, new Write(pos.immutable(), state, -1));
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
                Map<Long, Queue<Write>> dropped = PENDING.remove(level);
                if (dropped != null) dropped.values().forEach(q -> COUNT.addAndGet(-q.size()));
            }
        });
    }

    private static void apply(ServerLevel level, long key) {
        Map<Long, Queue<Write>> byChunk = PENDING.get(level);
        if (byChunk == null) return;
        Queue<Write> writes = byChunk.remove(key);
        if (writes == null) return;
        COUNT.addAndGet(-writes.size());
        for (Write w : writes) {
            try {
                if (w.state != null) {
                    level.setBlock(w.pos, w.state, 2);
                } else {
                    BlockState current = level.getBlockState(w.pos);
                    BlockState next = BlockBridge.withMeta(current, w.meta);
                    if (next != current) level.setBlock(w.pos, next, 2);
                }
            } catch (RuntimeException e) {
                LOG.debug("Deferred legacy generation write at {} failed", w.pos, e);
            }
        }
    }
}
