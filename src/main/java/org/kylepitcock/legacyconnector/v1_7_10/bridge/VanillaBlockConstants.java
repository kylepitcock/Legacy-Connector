package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Behaviour constants of 1.7.10's own block classes (render type of plants, opacity of glass, ...). The shim's vanilla
 * block classes are shells without vanilla logic, so {@code Block.getRenderType()} and friends answer from this table
 * (generated from the vanilla jar by tools/extract_block_constants.sh) by walking the class hierarchy to the nearest
 * vanilla class that overrode the method with a constant. Mod classes override normally and never get here.
 */
public final class VanillaBlockConstants {
    private static final String[] PREFIXES = {"legacyconnector.v1_7_10.", "legacyconnector_synth.v1_7_10."};
    private static final Map<String, Integer> TABLE = load();
    private static final ClassValue<Map<String, Integer>> RESOLVED = new ClassValue<>() {
        @Override
        protected Map<String, Integer> computeValue(Class<?> type) {
            return new java.util.concurrent.ConcurrentHashMap<>();
        }
    };

    private VanillaBlockConstants() {}

    public static int get(Class<?> cls, String srgMethod, int fallback) {
        Map<String, Integer> cache = RESOLVED.get(cls);
        Integer cached = cache.get(srgMethod);
        if (cached != null) return cached;
        int value = fallback;
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            String legacy = legacyName(c.getName());
            if (legacy == null) continue;
            Integer v = TABLE.get(legacy + " " + srgMethod);
            if (v != null) {
                value = v;
                break;
            }
        }
        cache.put(srgMethod, value);
        return value;
    }

    private static String legacyName(String className) {
        for (String p : PREFIXES) {
            if (className.startsWith(p)) return className.substring(p.length()).replace('.', '/');
        }
        return null;
    }

    private static Map<String, Integer> load() {
        Map<String, Integer> out = new HashMap<>();
        try (InputStream in = VanillaBlockConstants.class.getResourceAsStream("/legacyconnector/legacy/1.7.10/block-constants.txt")) {
            if (in == null) return out;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = r.readLine()) != null; ) {
                String[] p = line.split(" ");
                if (p.length == 3) out.put(p[0] + " " + p[1], Integer.parseInt(p[2]));
            }
        } catch (java.io.IOException | NumberFormatException e) {
            // no table: shells keep the base defaults
        }
        return out;
    }
}
