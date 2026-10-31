package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/**
 * 1.7.10 loaded a chunk and its entities in one step when a player was moved there; the modern game reads a chunk's entities
 * asynchronously, a few ticks after the chunk. Legacy code that teleports a player and then scans the world's entity list for what
 * it left there (Witchery's dream world looks for the sleeper's body in the overworld on waking, and the body stayed behind when
 * its chunk's entities had not arrived) saw none. Before the list is read the entities of the chunks around each player are
 * waited for (a few milliseconds at most, usually none).
 */
public final class EntityLoading {
    private static final long MAX_WAIT_MS = 250;

    private EntityLoading() {
    }

    /** Whether anything had to be waited for (the caller's cached entity list is then stale). */
    public static boolean awaitAround(ServerLevel level) {
        boolean waited = false;
        for (ServerPlayer player : level.players()) {
            int cx = player.chunkPosition().x, cz = player.chunkPosition().z;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) waited |= await(level, cx + dx, cz + dz);
            }
        }
        return waited;
    }

    private static boolean await(ServerLevel level, int cx, int cz) {
        try {
            var manager = level.entityManager;
            long key = ChunkPos.asLong(cx, cz);
            if (!pending(manager, key)) return false;
            long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
            while (pending(manager, key) && System.currentTimeMillis() < deadline) {
                manager.processPendingLoads();
                if (!pending(manager, key)) break;
                Thread.sleep(1);
            }
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
