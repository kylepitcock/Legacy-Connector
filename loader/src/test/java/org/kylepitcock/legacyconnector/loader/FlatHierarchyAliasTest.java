package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.synth.LegacyHierarchyData;

/** Synthesized classes of the flat-named 1.4.7 are named after the packaged class; their hierarchy entry must be found under that name too. */
class FlatHierarchyAliasTest {
    @Test
    void packagedNameFindsTheFlatEntryOf147() {
        LegacyHierarchyData data = LegacyHierarchyData.forVersion(LegacyVersion.V1_4_7);
        var flat = data.get("net/minecraft/src/ItemBucketMilk");
        var packaged = data.get("net/minecraft/item/ItemBucketMilk");
        assertNotNull(flat);
        assertNotNull(packaged);
        assertEquals(flat.superName(), packaged.superName());
        assertEquals(flat.access(), packaged.access());
    }

    @Test
    void otherVersionsAreUntouched() {
        LegacyHierarchyData data = LegacyHierarchyData.forVersion(LegacyVersion.V1_5_2);
        assertNotNull(data.get("net/minecraft/item/ItemBucketMilk"));
    }
}
