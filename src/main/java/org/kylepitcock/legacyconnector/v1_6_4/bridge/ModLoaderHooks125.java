package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

/**
 * The pure rules of ModLoader 1.2.5's tick hooks and chunk population, free of game types so they can be unit-tested.
 *
 * <p>{@code ModLoader.onTick} kept two ordered maps (mod to {@code useClock}) for {@code setInGameHook} / {@code setInGUIHook}. Per game render tick it
 * read the world time as the clock; a hook ran when the clock had moved since the last tick or when it was registered without {@code useClock}; a hook
 * whose callback returned false was removed (so a mod that never overrides {@code onTickInGame} ran once). {@code populateChunk} seeded one random
 * from the world seed and the chunk position and handed it to each mod's {@code generateSurface}/{@code generateNether}.
 */
public final class ModLoaderHooks125 {
    private ModLoaderHooks125() {}

    /** One ordered hook list (mod to useClock). */
    public static final class Hooks<M> {
        private final Map<M, Boolean> hooks = new LinkedHashMap<>();

        /** Registers (or re-registers with the new flag, keeping the position) a hook. */
        public synchronized void add(M mod, boolean useClock) {
            hooks.put(mod, useClock);
        }

        public synchronized void remove(M mod) {
            hooks.remove(mod);
        }

        public synchronized boolean contains(M mod) {
            return hooks.containsKey(mod);
        }

        public synchronized int size() {
            return hooks.size();
        }

        public synchronized boolean isEmpty() {
            return hooks.isEmpty();
        }

        /**
         * One tick: calls the hooks that are due and drops the ones whose callback answered false.
         *
         * @param clockChanged the world time differs from the one of the last tick (always true for a server tick)
         * @param call the mod's {@code onTickInGame}/{@code onTickInGUI}; true keeps the hook
         */
        public void run(boolean clockChanged, Predicate<M> call) {
            List<Map.Entry<M, Boolean>> snapshot;
            synchronized (this) {
                if (hooks.isEmpty()) return;
                snapshot = new ArrayList<>(hooks.entrySet());
            }
            boolean exact = Mc125Host.active(); // the other versions keep their hooks and ignore useClock, as before
            for (Map.Entry<M, Boolean> e : snapshot) {
                if (!exact) {
                    call.test(e.getKey());
                    continue;
                }
                if (!clockChanged && e.getValue()) continue;
                if (!call.test(e.getKey())) remove(e.getKey());
            }
        }
    }

    /** The random ModLoader's {@code populateChunk} gave the mods of a chunk: seeded from the world seed and the chunk position. */
    public static Random chunkRandom(long worldSeed, int chunkX, int chunkZ) {
        Random random = new Random(worldSeed);
        long a = random.nextLong() / 2L * 2L + 1L;
        long b = random.nextLong() / 2L * 2L + 1L;
        random.setSeed((long) chunkX * a + (long) chunkZ * b ^ worldSeed);
        return random;
    }
}
