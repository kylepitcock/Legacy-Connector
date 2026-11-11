package org.kylepitcock.legacyconnector.loader.transform;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Narrow resource-level compatibility rewrites for AlienEvo on Minecraft 1.21.1:
 * empty armor ItemStack guards, Omnitrix persistent-integer normalization, the
 * legacy ResourceLocation script constructor, typed SoundEvent playback, and
 * ResourceLocation/string normalization for the Omnitrix customization tab.
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
    static final String LEGACY_RESOURCE_LOCATION_HELPER =
            "const AlienEvoJavaResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation');\n\n"
                    + "function ResourceLocation(namespaceOrLocation, path) {\n"
                    + "    if (arguments.length > 1) {\n"
                    + "        return AlienEvoJavaResourceLocation.fromNamespaceAndPath(String(namespaceOrLocation), String(path));\n"
                    + "    }\n"
                    + "    return AlienEvoJavaResourceLocation.parse(String(namespaceOrLocation));\n"
                    + "}\n\n";

    static final String LEGACY_SOUND_EVENT_HELPER =
            "const AlienEvoSoundRegistry = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries').SOUND_EVENT;\n"
                    + "const AlienEvoSoundResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation');\n\n"
                    + "function getAlienEvoSoundEvent(soundId) {\n"
                    + "    const soundEvent = AlienEvoSoundRegistry.get(AlienEvoSoundResourceLocation.parse(String(soundId)));\n"
                    + "    if (soundEvent == null) {\n"
                    + "        throw new Error('AlienEvo sound event is not registered: ' + soundId);\n"
                    + "    }\n"
                    + "    return soundEvent;\n"
                    + "}\n\n";

    private static final String ALIEN_EVO_CYCLE = "addon/alienevo/kubejs_scripts/omnitrix_cycle.js";
    private static final String RESOURCE_LOCATION_HELPER_MARKER =
            "function ResourceLocation(namespaceOrLocation, path) {";
    private static final String SOUND_EVENT_HELPER_MARKER =
            "function getAlienEvoSoundEvent(soundId) {";


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
     * Apply narrowly scoped rewrites to the inspected AlienEvo resources.
     *
     * Missing or already migrated resources are no-ops. Every expected legacy
     * occurrence is counted exactly; unknown script shapes fail loudly rather
     * than leaving a partially or incorrectly rewritten callback.
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
        changed += rewriteLegacyScriptApis(resources);
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


    /**
     * Restore the legacy ResourceLocation constructor used by inspected
     * AlienEvo addon/client/data scripts. Also resolve cycle and transformation
     * sound IDs to registered SoundEvent instances before Java overload
     * selection, avoiding KubeJS/Rhino ambiguity between SoundEvent and Holder.
     *
     * @return number of distinct AlienEvo script resources changed
     */
    private static int rewriteLegacyScriptApis(Map<String, byte[]> resources) {
        int changed = 0;
        for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
            String path = entry.getKey();
            if (!isAlienEvoKubeJsScript(path)) continue;

            String script = new String(entry.getValue(), StandardCharsets.UTF_8);
            String migrated = script;

            if (migrated.contains("new ResourceLocation(")
                    && !migrated.contains(RESOURCE_LOCATION_HELPER_MARKER)) {
                migrated = insertAfterOmnitrixIntegerHelper(migrated, LEGACY_RESOURCE_LOCATION_HELPER);
            }

            if (ALIEN_EVO_CYCLE.equals(path)) {
                migrated = addSoundEventHelper(migrated);
                migrated = replaceExactScriptOccurrence(
                        path,
                        migrated,
                        "\`alienevo:\${soundToPlay}\`",
                        "getAlienEvoSoundEvent(\`alienevo:\${soundToPlay}\`)");
            } else if (ALIEN_EVO_TRANSFORM.equals(path)) {
                migrated = addSoundEventHelper(migrated);
                migrated = replaceExactScriptOccurrence(
                        path,
                        migrated,
                        "\`alienevo:\${transformSound}_transform\`",
                        "getAlienEvoSoundEvent(\`alienevo:\${transformSound}_transform\`)");
            }

            if (!migrated.equals(script)) {
                entry.setValue(migrated.getBytes(StandardCharsets.UTF_8));
                changed++;
            }
        }
        return changed;
    }

    private static boolean isAlienEvoKubeJsScript(String path) {
        return path.endsWith(".js")
                && (path.startsWith("addon/alienevo/kubejs_scripts/")
                        || path.startsWith("assets/alienevo/kubejs_scripts/")
                        || path.startsWith("data/alienevo/kubejs_scripts/"));
    }

    private static String insertAfterOmnitrixIntegerHelper(String script, String helper) {
        if (script.startsWith(OMNITRIX_INTEGER_HELPER)) {
            return OMNITRIX_INTEGER_HELPER
                    + helper
                    + script.substring(OMNITRIX_INTEGER_HELPER.length());
        }
        return helper + script;
    }

    private static String addSoundEventHelper(String script) {
        if (script.contains(SOUND_EVENT_HELPER_MARKER)) return script;
        return insertAfterOmnitrixIntegerHelper(script, LEGACY_SOUND_EVENT_HELPER);
    }

    private static String replaceExactScriptOccurrence(
            String path, String script, String legacy, String safe) {
        int legacyCount = occurrences(script, legacy);
        int safeCount = occurrences(script, safe);
        if (legacyCount == 0 && safeCount == 1) return script;
        if (legacyCount != 1 || safeCount != 0) {
            throw new IllegalStateException(
                    "AlienEvo resource " + path + " has an unexpected sound-event rewrite shape: expected "
                            + "1 legacy occurrence, found " + legacyCount + " legacy and " + safeCount + " migrated");
        }
        return script.replace(legacy, safe);
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
                        "AlienEvo resource " + path + " has an unexpected compatibility-rewrite shape: expected "
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
