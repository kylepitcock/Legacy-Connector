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
        assertEquals(a, ModLoaderHooks125.chunkRandom(42L, 3, -7).nextLong());
        assertTrue(a != ModLoaderHooks125.chunkRandom(42L, 4, -7).nextLong());
        assertTrue(a != ModLoaderHooks125.chunkRandom(43L, 3, -7).nextLong());
    }

    public static class Server {}

    public abstract static class Base {
        public boolean onTickInGame(Server s) {
            return false;
        }
    }

    public static class VoidMod extends Base {
        int ticks;

        /** the 1.2.5 server's shape: void, a different method from the shim's boolean one */
        public void onTickInGame(Server s, int unused) {}
    }

    public static class BooleanMod extends Base {
        int ticks;

        @Override
        public boolean onTickInGame(Server s) {
            return ++ticks < 3;
        }
    }

    /** a mod class compiled with a void server hook (hand-built: Java cannot declare both returns, so the shape is a subclass chain without the base) */
    public static class PlainVoid {
        int ticks;

        public void onTickInGame(Server s) {
            ticks++;
        }
    }

    @Test
    void serverTickCallsVoidAndBooleanOverrides() {
        PlainVoid v = new PlainVoid();
        assertTrue(ModLoaderHooks125.callServerTick(v, Base.class, Server.class, new Server()));
        assertEquals(1, v.ticks);
        BooleanMod b = new BooleanMod();
        assertTrue(ModLoaderHooks125.callServerTick(b, Base.class, Server.class, new Server()));
        assertTrue(ModLoaderHooks125.callServerTick(b, Base.class, Server.class, new Server()));
        assertFalse(ModLoaderHooks125.callServerTick(b, Base.class, Server.class, new Server()));
        assertEquals(3, b.ticks);
    }

    @Test
    void serverTickOfAModWithoutOverrideAnswersFalse() {
        assertFalse(ModLoaderHooks125.callServerTick(new VoidMod(), Base.class, Server.class, new Server()));
    }
}
