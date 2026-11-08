package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/**
 * 1.7.10 loaded a chunk and its entities in one step when a player was moved there; the modern game reads a chunk's entities
 * asynchronously, a few ticks after the chunk. Legacy code that teleports a player and then scans the world's entity list for what
 * it left there (Witchery's dream world looks for the sleeper's body in the overworld on waking, and the body stayed behind when
 * its chunk's entities had not arrived) saw none. Before the list is read the entities of the chunks around each player are
 * waited for (a few milliseconds at most, usually none). The wait is bounded twice: a call waits {@link #MAX_WAIT_MS} in total, and a chunk
 * that was still pending after waiting is not waited for again for {@link #COOLDOWN_MS} (a mod that reads the list every tick, such as
 * PortalGun's world tick, otherwise stalls the server thread for the whole time a chunk's entities stay pending).
 */
public final class EntityLoading {
    private static final long MAX_WAIT_MS = 250;
    private static final long COOLDOWN_MS = 10_000;
    private record Key(int level, long chunk) {}
    private static final java.util.Map<Key, Long> GAVE_UP = new java.util.concurrent.ConcurrentHashMap<>();

    private EntityLoading() {
    }

    /** Whether anything had to be waited for (the caller's cached entity list is then stale). */
    public static boolean awaitAround(ServerLevel level) {
        boolean waited = false;
        long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
        for (ServerPlayer player : level.players()) {
            int cx = player.chunkPosition().x, cz = player.chunkPosition().z;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) waited |= await(level, cx + dx, cz + dz, deadline);
            }
        }
        if (GAVE_UP.size() > 4096) GAVE_UP.clear();
        return waited;
    }

    private static boolean await(ServerLevel level, int cx, int cz, long deadline) {
        try {
            var manager = level.entityManager;
            long chunk = ChunkPos.asLong(cx, cz);
            if (!pending(manager, chunk)) return false;
            Key key = new Key(System.identityHashCode(level), chunk);
            long now = System.currentTimeMillis();
            Long until = GAVE_UP.get(key);
            if (until != null && now < until) return false;
            while (pending(manager, chunk) && System.currentTimeMillis() < deadline) {
                manager.processPendingLoads();
                if (!pending(manager, chunk)) break;
                Thread.sleep(1);
            }
            if (pending(manager, chunk)) GAVE_UP.put(key, System.currentTimeMillis() + COOLDOWN_MS);
            return true;
        } catch (RuntimeException | LinkageError | InterruptedException e) {
            return false;
        }
    }

    private static boolean pending(net.minecraft.world.level.entity.PersistentEntitySectionManager<?> manager, long key) {
        var status = manager.chunkLoadStatuses.get(key);
        return status != null && String.valueOf(status).equals("PENDING");
    }
}
