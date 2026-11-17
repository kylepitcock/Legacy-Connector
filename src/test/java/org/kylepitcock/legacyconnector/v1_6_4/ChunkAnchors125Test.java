package org.kylepitcock.legacyconnector.v1_6_4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.ChunkAnchors125;

class ChunkAnchors125Test {
    @Test
    void keyRoundTripsNegativeCoordinates() {
        long k = ChunkAnchors125.key(-3, 12);
        assertEquals(-3, ChunkAnchors125.x(k));
        assertEquals(12, ChunkAnchors125.z(k));
        long m = ChunkAnchors125.key(5, -9);
        assertEquals(5, ChunkAnchors125.x(m));
        assertEquals(-9, ChunkAnchors125.z(m));
    }

    @Test
    void forcesNewChunksOnceAndReleasesDroppedOnes() {
        Set<Long> held = new HashSet<>();
        List<String> log = new ArrayList<>();
        long a = ChunkAnchors125.key(0, 0);
        long b = ChunkAnchors125.key(1, 0);
        ChunkAnchors125.reconcile(held, List.of(a, b), k -> log.add("+" + ChunkAnchors125.x(k)), k -> log.add("-" + ChunkAnchors125.x(k)));
        ChunkAnchors125.reconcile(held, List.of(a, b), k -> log.add("+" + ChunkAnchors125.x(k)), k -> log.add("-" + ChunkAnchors125.x(k)));
        assertEquals(List.of("+0", "+1"), log);
        ChunkAnchors125.reconcile(held, List.of(b), k -> log.add("+" + ChunkAnchors125.x(k)), k -> log.add("-" + ChunkAnchors125.x(k)));
        assertEquals(List.of("+0", "+1", "-0"), log);
        assertEquals(Set.of(b), held);
    }
}
