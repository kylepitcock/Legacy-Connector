package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Forge 3.x's {@code IChunkLoadHandler.addActiveChunks} named the chunks to keep active each tick (Railcraft's anchors). The layer keeps them loaded
 * with the world's forced chunks (what a modern chunk-loading mod uses); this is the pure bookkeeping: force what is newly asked, release what is no
 * longer asked, once each.
 */
public final class ChunkAnchors125 {
    private ChunkAnchors125() {}

    public static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public static int x(long key) {
        return (int) (key >> 32);
    }

    public static int z(long key) {
        return (int) key;
    }

    /** Brings {@code held} to {@code wanted}: {@code force} for each key added, {@code release} for each key dropped. */
    public static <K> void reconcile(Set<K> held, Collection<K> wanted, Consumer<K> force, Consumer<K> release) {
        Set<K> want = new HashSet<>(wanted);
        for (K k : want) if (held.add(k)) force.accept(k);
        for (Iterator<K> it = held.iterator(); it.hasNext();) {
            K k = it.next();
            if (!want.contains(k)) {
                it.remove();
                release.accept(k);
            }
        }
    }
}
