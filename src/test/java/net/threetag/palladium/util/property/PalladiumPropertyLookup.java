package net.threetag.palladium.util.property;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Test-only stand-in for PalladiumPropertyLookup's inspected reflective API. */
public final class PalladiumPropertyLookup {
    public record Property(String type, String key) {
    }

    private static final Map<String, Function<String, Object>> FACTORIES = new HashMap<>();

    static {
        reset();
    }

    private PalladiumPropertyLookup() {
    }

    public static void register(String type, Function<String, Object> factory) {
        FACTORIES.put(type, factory);
    }

    public static Object get(String type, String key) {
        Function<String, Object> factory = FACTORIES.get(type);
        return factory == null ? null : factory.apply(key);
    }

    public static void reset() {
        FACTORIES.clear();
        register("integer", key -> new Property("integer", key));
    }
}
