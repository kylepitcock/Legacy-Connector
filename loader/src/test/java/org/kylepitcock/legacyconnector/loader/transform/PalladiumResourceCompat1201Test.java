package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Regression tests for AlienEvo armor, Omnitrix property, and customization UI rewrites. */
class PalladiumResourceCompat1201Test {
    private static void put(Map<String, byte[]> resources, String path, String script) {
        resources.put(path, script.getBytes(StandardCharsets.UTF_8));
    }

    private static String get(Map<String, byte[]> resources, String path) {
        return new String(resources.get(path), StandardCharsets.UTF_8);
    }

    @Test
    void migratesAlienEvoArmorTransformAndPowerUiPathsAndIsIdempotent() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_STORE_ARMOR, """
                armorSlots.forEach(slot => {
                    if (entity[slot + 'ArmorItem']) {
                        hasArmor = true;
                        entity.persistentData.put(slot + 'ArmorStore', entity[slot + 'ArmorItem']);
                    }
                });
                """);
        String slotTransform = """
                let hasAnyArmor = armorSlots.some(slot => {
                    let armorItem = player[slot + 'ArmorItem'];
                    return armorItem && armorItem.id !== 'minecraft:air';
                });
                armorSlots.forEach(slot => {
                    let armorItem = player[slot + 'ArmorItem'];
                    if (armorItem && armorItem.id !== 'minecraft:air') {
                        player.persistentData.put(slot + 'ArmorStore', armorItem);
                    }
                });
                """;
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_SLOT_TRANSFORM, slotTransform);
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_QUICK_CHANGE, slotTransform);
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_POWER_UI, """
                if (currentTab === omnitrixTab) {}
                if (currentTab === omnitrixTab) {}
                if (currentTab === omnitrixTab) {}
                if (e.tab === omnitrixTab) {}
                if (e.tab === omnitrixTab) {}
                if (e.tab === omnitrixTab) {}
                if (e.tab === omnitrixTab) {}
                if (e.tab === omnitrixTab) {}
                """);
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_TRANSFORM, """
                function getRandomInt(min, max) {
                    return 1;
                }
                palladium.setProperty(entity, omnitrixPropertyKey, newAlienUnlocked);
                palladium.setProperty(entity, omnitrixPropertyKey, newAlienUnlocked);
                palladium.setProperty(entity, omnitrixPropertyKey, selectedAlienNum);
                palladium.setProperty(entity, omnitrixPropertyKey, selectedAlienNum);
                palladium.setProperty(entity, omnitrixPropertyKey, alienNumber);
                palladium.setProperty(entity, omnitrixPropertyKey, alien_num);
                """);

        assertEquals(5, PalladiumResourceCompat1201.apply(resources));

        String storeArmor = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_STORE_ARMOR);
        assertTrue(storeArmor.contains(PalladiumResourceCompat1201.SAFE_STORE_ARMOR_CHECK));
        assertFalse(storeArmor.contains(PalladiumResourceCompat1201.LEGACY_STORE_ARMOR_CHECK));

        for (String path : new String[] {
                PalladiumResourceCompat1201.ALIEN_EVO_SLOT_TRANSFORM,
                PalladiumResourceCompat1201.ALIEN_EVO_QUICK_CHANGE
        }) {
            String script = get(resources, path);
            assertTrue(script.contains(PalladiumResourceCompat1201.SAFE_ARMOR_ID_RETURN));
            assertTrue(script.contains(PalladiumResourceCompat1201.SAFE_ARMOR_ID_CHECK));
            assertFalse(script.contains(PalladiumResourceCompat1201.LEGACY_ARMOR_ID_RETURN));
            assertFalse(script.contains(PalladiumResourceCompat1201.LEGACY_ARMOR_ID_CHECK));
        }

        String transform = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_TRANSFORM);
        assertTrue(transform.startsWith(PalladiumResourceCompat1201.OMNITRIX_INTEGER_HELPER));
        assertEquals(6, transform.split("palladium\\.setProperty\\(entity, omnitrixPropertyKey, toOmnitrixInteger\\(", -1).length - 1);
        assertFalse(transform.contains("palladium.setProperty(entity, omnitrixPropertyKey, selectedAlienNum);"));
        assertFalse(transform.contains("palladium.setProperty(entity, omnitrixPropertyKey, alienNumber);"));
        assertFalse(transform.contains("palladium.setProperty(entity, omnitrixPropertyKey, alien_num);"));
        assertFalse(transform.contains("palladium.setProperty(entity, omnitrixPropertyKey, newAlienUnlocked);"));

        String powerUi = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_POWER_UI);
        assertEquals(3, powerUi.split("currentTab.toString() === omnitrixTab", -1).length - 1);
        assertEquals(5, powerUi.split("e.tab.toString() === omnitrixTab", -1).length - 1);
        assertFalse(powerUi.contains("currentTab === omnitrixTab"));
        assertFalse(powerUi.contains("e.tab === omnitrixTab"));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
    }

    @Test
    void doesNotTouchOtherResourcesOrUnrelatedScripts() {
        Map<String, byte[]> resources = new HashMap<>();
        String unrelated = "if (entity[slot + 'ArmorItem']) { }";
        put(resources, "addon/example/kubejs_scripts/store_armor.js", unrelated);
        put(resources, "data/example/kubejs_scripts/quick_change.js",
                "return armorItem && armorItem.id !== 'minecraft:air';");

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(unrelated, get(resources, "addon/example/kubejs_scripts/store_armor.js"));
        assertEquals("return armorItem && armorItem.id !== 'minecraft:air';",
                get(resources, "data/example/kubejs_scripts/quick_change.js"));
    }

    @Test
    void toleratesAlreadyUpdatedAndMissingAlienEvoScripts() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_STORE_ARMOR,
                "if (entity[slot + 'ArmorItem'] && !entity[slot + 'ArmorItem'].isEmpty()) { }");
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_SLOT_TRANSFORM,
                "return armorItem && !armorItem.isEmpty(); if (armorItem && !armorItem.isEmpty()) { }");

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertFalse(resources.containsKey(PalladiumResourceCompat1201.ALIEN_EVO_QUICK_CHANGE));
    }
}
