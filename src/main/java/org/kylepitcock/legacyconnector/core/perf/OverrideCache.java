package org.kylepitcock.legacyconnector.core.perf;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code perf.hookOverrideCache}: "does this legacy class override method X(params)" answers, per class and method name, found
 * without allocating. The hosts of legacy entities ask it for hooks that run every tick (isPushedByWater, updateRidden,
 * decreaseAirSupply, collideWithEntity...); the old cache built a string key ({@code name + Arrays.toString(params)}) and a
 * capturing lambda on every call, cached or not.
 */
public final class OverrideCache {
    /** The uncached question (walks the class hierarchy). */
    @FunctionalInterface
    public interface Scan {
        boolean overrides(Class<?> cls, String name, Class<?>[] params);
    }

    private record Known(Class<?>[] params, boolean value) {}

    private final ClassValue<ConcurrentHashMap<String, Known[]>> byClass = new ClassValue<>() {
        @Override
        protected ConcurrentHashMap<String, Known[]> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>(4);
        }
    };

    public boolean get(Class<?> cls, String name, Class<?>[] params, Scan scan) {
        ConcurrentHashMap<String, Known[]> byName = byClass.get(cls);
        Known[] known = byName.get(name);
        if (known != null) {
            for (Known k : known) {
                if (Arrays.equals(k.params, params)) return k.value;
            }
        }
        boolean value = scan.overrides(cls, name, params);
        Known entry = new Known(params.clone(), value);
        byName.merge(name, new Known[] {entry}, (old, add) -> {
            for (Known k : old) if (Arrays.equals(k.params, entry.params)) return old;
            Known[] out = Arrays.copyOf(old, old.length + 1);
            out[old.length] = entry;
            return out;
        });
        return value;
    }
}
