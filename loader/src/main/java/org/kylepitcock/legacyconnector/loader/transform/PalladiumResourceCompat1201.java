package org.kylepitcock.legacyconnector.loader.transform;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Narrow resource-level compatibility rewrites for known 1.20.1 KubeJS addon
 * scripts whose behavior is unsafe with the 1.21.1 ItemStack representation.
 */
final class PalladiumResourceCompat1201 {
    static final String ALIEN_EVO_STORE_ARMOR =
            "addon/alienevo/kubejs_scripts/store_armor.js";
    static final String LEGACY_EMPTY_ARMOR_CHECK =
            "if (entity[slot + 'ArmorItem']) {";
    static final String SAFE_EMPTY_ARMOR_CHECK =
            "if (entity[slot + 'ArmorItem'] && !entity[slot + 'ArmorItem'].isEmpty()) {";

    private PalladiumResourceCompat1201() {}

    /**
     * Do not serialize ItemStack.EMPTY into persistent NBT. KubeJS 7's 1.21.1
     * ItemStack codec correctly rejects an empty (air, count 0) stack, while
     * the old script's Java-object truthiness check treated it as non-null.
     *
     * Returns the number of resource scripts changed. Missing or already
     * updated resources are no-ops, so unrelated legacy mods are unaffected.
     */
    static int apply(Map<String, byte[]> resources) {
        byte[] original = resources.get(ALIEN_EVO_STORE_ARMOR);
        if (original == null) return 0;

        String script = new String(original, StandardCharsets.UTF_8);
        if (script.contains(SAFE_EMPTY_ARMOR_CHECK)) return 0;

        int matches = occurrences(script, LEGACY_EMPTY_ARMOR_CHECK);
        if (matches == 0) return 0;
        if (matches != 1) {
            throw new IllegalStateException(
                    "AlienEvo store_armor.js has an unexpected number of legacy empty-stack checks: " + matches);
        }

        String migrated = script.replace(LEGACY_EMPTY_ARMOR_CHECK, SAFE_EMPTY_ARMOR_CHECK);
        resources.put(ALIEN_EVO_STORE_ARMOR, migrated.getBytes(StandardCharsets.UTF_8));
        return 1;
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int offset = 0; (offset = text.indexOf(needle, offset)) >= 0; offset += needle.length()) {
            count++;
        }
        return count;
    }
}
