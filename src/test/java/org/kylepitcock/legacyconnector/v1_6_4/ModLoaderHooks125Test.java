package org.kylepitcock.legacyconnector.v1_6_4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.ModLoaderHooks125;

/** ModLoader 1.2.5's tick-hook rules and chunk random, on fake mods. */
class ModLoaderHooks125Test {
    @org.junit.jupiter.api.BeforeAll
    static void mc125On() {
        System.setProperty(org.kylepitcock.legacyconnector.v1_6_4.bridge.Mc125Host.PROPERTY, "true"); // the loader sets it when it hosts a 1.2.5 jar
    }

    @org.junit.jupiter.api.AfterAll
    static void mc125Off() {
        System.clearProperty(org.kylepitcock.legacyconnector.v1_6_4.bridge.Mc125Host.PROPERTY);
    }

    @Test
    void clockHooksRunOncePerWorldTickAndOthersEveryFrame() {
        ModLoaderHooks125.Hooks<String> hooks = new ModLoaderHooks125.Hooks<>();
        hooks.add("clock", true);
        hooks.add("frame", false);
        List<String> calls = new ArrayList<>();
        hooks.run(false, m -> calls.add(m));
        assertEquals(List.of("frame"), calls);
        hooks.run(true, m -> calls.add(m));
        assertEquals(List.of("frame", "clock", "frame"), calls);
    }

    @Test
    void falseUnregistersAndNeverRunsAgain() {
        ModLoaderHooks125.Hooks<String> hooks = new ModLoaderHooks125.Hooks<>();
        hooks.add("once", false);
        hooks.add("always", false);
        List<String> calls = new ArrayList<>();
        hooks.run(true, m -> {
            calls.add(m);
            return !m.equals("once");
        });
        hooks.run(true, m -> {
            calls.add(m);
            return true;
        });
        assertEquals(List.of("once", "always", "always"), calls);
        assertFalse(hooks.contains("once"));
        assertEquals(1, hooks.size());
    }

    @Test
    void hookMayUnregisterItselfWhileRunning() {
        ModLoaderHooks125.Hooks<String> hooks = new ModLoaderHooks125.Hooks<>();
        hooks.add("a", false);
        hooks.add("b", false);
        hooks.run(true, m -> {
            hooks.remove("b");
            return true;
        });
        assertFalse(hooks.contains("b"));
    }

    @Test
    void chunkRandomIsDeterministicPerChunkAndSeed() {
        long a = ModLoaderHooks125.chunkRandom(42L, 3, -7).nextLong();
        // the value ModLoader 1.2.5's populateChunk bytecode gives (nextLong()/2*2+1 twice, then (cx*a + cz*b) ^ seed), computed independently
        assertEquals(-3401055855222692752L, a);
        assertEquals(a, ModLoaderHooks125.chunkRandom(42L, 3, -7).nextLong());
        assertTrue(a != ModLoaderHooks125.chunkRandom(42L, 4, -7).nextLong());
        assertTrue(a != ModLoaderHooks125.chunkRandom(43L, 3, -7).nextLong());
    }
}
