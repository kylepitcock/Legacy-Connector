package org.kylepitcock.legacyconnector.loader.transform;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Evidence-based cleanup for AlienEvo's hidden, unreferenced code-system test
 * power. The shipping code system is implemented by the registered
 * alienevo:display_code ability; this hidden test resource names two ability
 * types that are not registered anywhere in the JAR.
 */
final class AlienEvoResourceCompat1201 {
    static final String ORPHAN_CODE_POWER =
            "data/alienevo/palladium/powers/code_power_test.json";

    private AlienEvoResourceCompat1201() {}

    /**
     * Remove only the exact hidden test power when its two unregistered types
     * and the power itself have no references from any other resource/class.
     * A future AlienEvo build that makes it real or references it is preserved.
     */
    static boolean removeUnreferencedHiddenCodePower(
            Map<String, byte[]> resources, Map<String, byte[]> classes) {
        byte[] original = resources.get(ORPHAN_CODE_POWER);
        if (original == null) return false;

        JsonObject power;
        try {
            JsonElement parsed = JsonParser.parseString(new String(original, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return false;
            power = parsed.getAsJsonObject();
        } catch (RuntimeException ignored) {
            return false;
        }

        if (!power.has("hidden") || !power.get("hidden").isJsonPrimitive()
                || !power.get("hidden").getAsBoolean()) return false;

        JsonObject abilities = power.has("abilities") && power.get("abilities").isJsonObject()
                ? power.getAsJsonObject("abilities") : null;
        if (abilities == null
                || !"alienevo:generate_random".equals(abilityType(abilities, "generate_code"))
                || !"alienevo:check_code".equals(abilityType(abilities, "check_code"))) {
            return false;
        }

        if (containsOutside(resources, ORPHAN_CODE_POWER, "code_power_test")
                || containsOutside(resources, ORPHAN_CODE_POWER, "alienevo:generate_random")
                || containsOutside(resources, ORPHAN_CODE_POWER, "alienevo:check_code")
                || contains(classes, "code_power_test")
                || contains(classes, "alienevo:generate_random")
                || contains(classes, "alienevo:check_code")) {
            return false;
        }

        resources.remove(ORPHAN_CODE_POWER);
        return true;
    }

    private static String abilityType(JsonObject abilities, String id) {
        if (!abilities.has(id) || !abilities.get(id).isJsonObject()) return null;
        JsonObject ability = abilities.getAsJsonObject(id);
        return ability.has("type") && ability.get("type").isJsonPrimitive()
                ? ability.get("type").getAsString() : null;
    }

    private static boolean containsOutside(Map<String, byte[]> values, String excludedPath, String token) {
        byte[] needle = token.getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<String, byte[]> entry : values.entrySet()) {
            if (!entry.getKey().equals(excludedPath) && contains(entry.getValue(), needle)) return true;
        }
        return false;
    }

    private static boolean contains(Map<String, byte[]> values, String token) {
        byte[] needle = token.getBytes(StandardCharsets.UTF_8);
        for (byte[] value : values.values()) if (contains(value, needle)) return true;
        return false;
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        if (needle.length == 0 || haystack.length < needle.length) return false;
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }
}
