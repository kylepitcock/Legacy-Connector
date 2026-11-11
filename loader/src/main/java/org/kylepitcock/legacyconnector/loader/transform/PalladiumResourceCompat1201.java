package org.kylepitcock.legacyconnector.loader.transform;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Narrow resource-level compatibility rewrites for AlienEvo's armor storage.
 * Empty ItemStacks are truthy Java objects in Rhino, but must not be encoded
 * as persistent NBT on Minecraft 1.21.1.
 */
final class PalladiumResourceCompat1201 {
    static final String ALIEN_EVO_STORE_ARMOR =
            "addon/alienevo/kubejs_scripts/store_armor.js";
    static final String ALIEN_EVO_SLOT_TRANSFORM =
            "addon/alienevo/kubejs_scripts/slot_transform.js";
    static final String ALIEN_EVO_QUICK_CHANGE =
            "data/alienevo/kubejs_scripts/quick_change.js";

    static final String LEGACY_STORE_ARMOR_CHECK =
            "if (entity[slot + 'ArmorItem']) {";
    static final String SAFE_STORE_ARMOR_CHECK =
            "if (entity[slot + 'ArmorItem'] && !entity[slot + 'ArmorItem'].isEmpty()) {";

    static final String LEGACY_ARMOR_ID_RETURN =
            "return armorItem && armorItem.id !== 'minecraft:air';";
    static final String SAFE_ARMOR_ID_RETURN =
            "return armorItem && !armorItem.isEmpty();";
    static final String LEGACY_ARMOR_ID_CHECK =
            "if (armorItem && armorItem.id !== 'minecraft:air') {";
    static final String SAFE_ARMOR_ID_CHECK =
            "if (armorItem && !armorItem.isEmpty()) {";

    private PalladiumResourceCompat1201() {}

    /**
     * Do not serialize ItemStack.EMPTY into persistent NBT. Apply only to the
     * three inspected AlienEvo resources which store player armor during an
     * Omnitrix transformation or quick-change transition.
     *
     * Missing or already migrated resources are no-ops. Every expected legacy
     * check is counted exactly; unknown script shapes fail loudly rather than
     * leaving a partially or incorrectly rewritten callback.
     *
     * @return number of resource scripts changed
     */
    static int apply(Map<String, byte[]> resources) {
        int changed = 0;
        changed += rewrite(
                resources,
                ALIEN_EVO_STORE_ARMOR,
                new String[][] {{LEGACY_STORE_ARMOR_CHECK, SAFE_STORE_ARMOR_CHECK, "1"}});
        changed += rewrite(
                resources,
                ALIEN_EVO_SLOT_TRANSFORM,
                new String[][] {
                        {LEGACY_ARMOR_ID_RETURN, SAFE_ARMOR_ID_RETURN, "1"},
                        {LEGACY_ARMOR_ID_CHECK, SAFE_ARMOR_ID_CHECK, "1"}
                });
        changed += rewrite(
                resources,
                ALIEN_EVO_QUICK_CHANGE,
                new String[][] {
                        {LEGACY_ARMOR_ID_RETURN, SAFE_ARMOR_ID_RETURN, "1"},
                        {LEGACY_ARMOR_ID_CHECK, SAFE_ARMOR_ID_CHECK, "1"}
                });
        return changed;
    }

    private static int rewrite(Map<String, byte[]> resources, String path, String[][] replacements) {
        byte[] original = resources.get(path);
        if (original == null) return 0;

        String script = new String(original, StandardCharsets.UTF_8);
        String migrated = script;
        boolean changed = false;

        for (String[] replacement : replacements) {
            String legacy = replacement[0];
            String safe = replacement[1];
            int expected = Integer.parseInt(replacement[2]);
            int legacyCount = occurrences(migrated, legacy);
            int safeCount = occurrences(migrated, safe);

            if (legacyCount == 0 && safeCount == expected) continue;
            if (legacyCount != expected || safeCount != 0) {
                throw new IllegalStateException(
                        "AlienEvo resource " + path + " has an unexpected armor ItemStack guard: expected "
                                + expected + " legacy occurrence(s), found " + legacyCount
                                + " legacy and " + safeCount + " migrated");
            }

            migrated = migrated.replace(legacy, safe);
            changed = true;
        }

        if (changed) {
            resources.put(path, migrated.getBytes(StandardCharsets.UTF_8));
            return 1;
        }
        return 0;
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int offset = 0; (offset = text.indexOf(needle, offset)) >= 0; offset += needle.length()) {
            count++;
        }
        return count;
    }
}
