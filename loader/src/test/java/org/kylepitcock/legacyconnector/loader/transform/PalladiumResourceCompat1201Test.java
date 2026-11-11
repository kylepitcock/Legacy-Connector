package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Regression tests for empty armor ItemStacks in AlienEvo's store_armor ability. */
class PalladiumResourceCompat1201Test {
    @Test
    void skipsEmptyArmorStacksBeforeStoringThemInPersistentNbt() {
        Map<String, byte[]> resources = new HashMap<>();
        String script = """
                armorSlots.forEach(slot => {
                    if (entity[slot + 'ArmorItem']) {
                        hasArmor = true;
                        entity.persistentData.put(slot + 'ArmorStore', entity[slot + 'ArmorItem']);
                    }
                });
                """;
        resources.put(
                PalladiumResourceCompat1201.ALIEN_EVO_STORE_ARMOR,
                script.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, PalladiumResourceCompat1201.apply(resources));

        String migrated = new String(
                resources.get(PalladiumResourceCompat1201.ALIEN_EVO_STORE_ARMOR),
                StandardCharsets.UTF_8);
        assertTrue(migrated.contains(PalladiumResourceCompat1201.SAFE_EMPTY_ARMOR_CHECK));
        assertFalse(migrated.contains(PalladiumResourceCompat1201.LEGACY_EMPTY_ARMOR_CHECK));

        // Re-running the resource pass must not change an already migrated script.
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
    }

    @Test
    void doesNotTouchOtherResourcesOrUnrelatedScripts() {
        Map<String, byte[]> resources = new HashMap<>();
        String unrelated = "if (entity[slot + 'ArmorItem']) { }";
        resources.put("addon/example/kubejs_scripts/store_armor.js",
                unrelated.getBytes(StandardCharsets.UTF_8));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(unrelated, new String(
                resources.get("addon/example/kubejs_scripts/store_armor.js"),
                StandardCharsets.UTF_8));
    }

    @Test
    void toleratesAnAlienEvoScriptThatDoesNotUseTheLegacyEmptyCheck() {
        Map<String, byte[]> resources = new HashMap<>();
        String updated = "if (armorItem && !armorItem.isEmpty()) { }";
        resources.put(PalladiumResourceCompat1201.ALIEN_EVO_STORE_ARMOR,
                updated.getBytes(StandardCharsets.UTF_8));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(updated, new String(
                resources.get(PalladiumResourceCompat1201.ALIEN_EVO_STORE_ARMOR),
                StandardCharsets.UTF_8));
    }
}
