package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Regression test for #182 (SRParasites plus a large mod: 80% of the server tick went into failed class lookups, because
 * EntitySpawnPlacementRegistry.getPlacementForEntity runs per entity type per chunk per tick).
 */
class RuntimeApis1122SynthClassTest {
    @Test
    void shimClassIsFoundAndCached() {
        Class<?> a = RuntimeApis1122.synthClass("net.minecraft.entity.EntityLiving$SpawnPlacementType");
        assertNotNull(a);
        assertSame(a, RuntimeApis1122.synthClass("net.minecraft.entity.EntityLiving$SpawnPlacementType"));
    }

    @Test
    void missingClassIsNotLookedUpAgainEveryCall() {
        String name = "net.minecraft.does.not.Exist";
        assertNull(RuntimeApis1122.synthClass(name));
        long start = System.nanoTime();
        for (int i = 0; i < 100_000; i++) assertNull(RuntimeApis1122.synthClass(name));
        long ms = (System.nanoTime() - start) / 1_000_000;
        // a real lookup (stack walk plus class loader misses) costs 10 us or more: 100k of them would take over a second
        assertTrue(ms < 500, "100k repeated misses took " + ms + " ms");
    }
}
