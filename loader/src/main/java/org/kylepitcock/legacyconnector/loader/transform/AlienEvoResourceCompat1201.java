package org.kylepitcock.legacyconnector.loader.transform;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Evidence-based resource repairs for AlienEvo: cleanup of its hidden,
 * unreferenced code-system test power, and marking its Ad Astra damage-type
 * tag entry optional so the JAR remains loadable when that integration mod
 * is absent. The shipping code system uses the registered
 * alienevo:display_code ability.
 */
final class AlienEvoResourceCompat1201 {
    static final String ORPHAN_CODE_POWER =
            "data/alienevo/palladium/powers/code_power_test.json";
    static final String EVOASTRA_DAMAGE_TAG =
            "data/alienevo/tags/damage_type/evoastra.json";
    static final String ADDON_RESOURCE_ROOT_MARKER = "addon/.mcassetsroot";

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

    /**
     * The tag provides an optional cross-mod damage type, not a mandatory
     * AlienEvo dependency. Preserve the entry when Ad Astra is installed, but
     * let NeoForge skip it cleanly when the registry entry is unavailable.
     */
    static boolean makeAdAstraDamageTypeOptional(Map<String, byte[]> resources) {
        byte[] original = resources.get(EVOASTRA_DAMAGE_TAG);
        if (original == null) return false;
        try {
            JsonElement parsed = JsonParser.parseString(new String(original, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return false;
            JsonObject root = parsed.getAsJsonObject();
            if (!root.has("values") || !root.get("values").isJsonArray()) return false;
            JsonArray values = root.getAsJsonArray("values");
            boolean changed = false;
            for (int i = 0; i < values.size(); i++) {
                JsonElement value = values.get(i);
                if (value.isJsonPrimitive() && value.getAsString().equals("ad_astra:oxygen")) {
                    JsonObject optional = new JsonObject();
                    optional.addProperty("id", "ad_astra:oxygen");
                    optional.addProperty("required", false);
                    values.set(i, optional);
                    changed = true;
                }
            }
            if (changed) resources.put(EVOASTRA_DAMAGE_TAG, root.toString().getBytes(StandardCharsets.UTF_8));
            return changed;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /**
     * AlienEvo stores most client assets under a nonstandard {@code addon/}
     * root. Vanilla's 1.21.1 pack loader expects a marker at that root when
     * discovering it as a resource namespace.
     */
    static boolean ensureAddonResourceRootMarker(Map<String, byte[]> resources) {
        boolean hasAddonRoot = resources.keySet().stream()
                .anyMatch(path -> path.startsWith("addon/") && !path.equals(ADDON_RESOURCE_ROOT_MARKER));
        if (!hasAddonRoot || resources.containsKey(ADDON_RESOURCE_ROOT_MARKER)) return false;
        resources.put(ADDON_RESOURCE_ROOT_MARKER, new byte[0]);
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
            if (!entry.getKey().equals(excludedPath)
                    && (entry.getKey().contains(token) || contains(entry.getValue(), needle))) return true;
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
