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
    static final String ALIEN_EVO_TRANSFORM =
            "addon/alienevo/kubejs_scripts/transform.js";
    static final String ALIEN_EVO_POWER_UI =
            "assets/alienevo/kubejs_scripts/power_ui.js";

    static final String OMNITRIX_INTEGER_HELPER =
            "const JavaInteger = Java.loadClass('java.lang.Integer');\n\n"
                    + "function toOmnitrixInteger(value) {\n"
                    + "    if (value != null && typeof value.getAsInt === 'function') {\n"
                    + "        return JavaInteger.valueOf(value.getAsInt());\n"
                    + "    }\n"
                    + "    return JavaInteger.valueOf(Math.trunc(Number(value)));\n"
                    + "}\n\n";

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

    private static final String[][] POWER_UI_TAB_COMPARISONS = {
            {
                    "currentTab === omnitrixTab",
                    "currentTab.toString() === omnitrixTab",
                    "3"
            },
            {
                    "e.tab === omnitrixTab",
                    "e.tab.toString() === omnitrixTab",
                    "5"
            }
    };

    private static final String[][] OMNITRIX_INTEGER_WRITES = {
            {
                    "palladium.setProperty(entity, omnitrixPropertyKey, newAlienUnlocked);",
                    "palladium.setProperty(entity, omnitrixPropertyKey, toOmnitrixInteger(newAlienUnlocked));",
                    "2"
            },
            {
                    "palladium.setProperty(entity, omnitrixPropertyKey, selectedAlienNum);",
                    "palladium.setProperty(entity, omnitrixPropertyKey, toOmnitrixInteger(selectedAlienNum));",
                    "2"
            },
            {
                    "palladium.setProperty(entity, omnitrixPropertyKey, alienNumber);",
                    "palladium.setProperty(entity, omnitrixPropertyKey, toOmnitrixInteger(alienNumber));",
                    "1"
            },
            {
                    "palladium.setProperty(entity, omnitrixPropertyKey, alien_num);",
                    "palladium.setProperty(entity, omnitrixPropertyKey, toOmnitrixInteger(alien_num));",
                    "1"
            }
    };

    private PalladiumResourceCompat1201() {}

    /**
     * Do not serialize ItemStack.EMPTY into persistent NBT. Apply only to the
     * three inspected armor-storage resources plus the Omnitrix transform script which store player armor during an
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
        changed += rewriteTransformScript(resources);
        changed += rewrite(resources, ALIEN_EVO_POWER_UI, POWER_UI_TAB_COMPARISONS);
        return changed;
    }

    /**
     * KubeJS 7 exposes numeric persistent-data entries as NBT tags. Palladium's
     * IntegerProperty accepts Number values, not IntTag instances, so normalize
     * the six Omnitrix-cycle writes to java.lang.Integer before calling it.
     */
    private static int rewriteTransformScript(Map<String, byte[]> resources) {
        byte[] original = resources.get(ALIEN_EVO_TRANSFORM);
        if (original == null) return 0;

        String script = new String(original, StandardCharsets.UTF_8);
        boolean helperAdded = false;
        if (!script.startsWith(OMNITRIX_INTEGER_HELPER)) {
            String anchor = "function getRandomInt(min, max) {";
            if (!script.startsWith(anchor)) {
                throw new IllegalStateException(
                        "AlienEvo transform.js has an unexpected prologue; cannot add integer normalization safely");
            }
            script = OMNITRIX_INTEGER_HELPER + script;
            resources.put(ALIEN_EVO_TRANSFORM, script.getBytes(StandardCharsets.UTF_8));
            helperAdded = true;
        }

        int rewritten = rewrite(resources, ALIEN_EVO_TRANSFORM, OMNITRIX_INTEGER_WRITES);
        return helperAdded || rewritten > 0 ? 1 : 0;
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
