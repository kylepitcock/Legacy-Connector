package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.item.ArmorItem;
import legacyconnector.vb1_7_3.net.minecraft.src.ModLoader;
import org.junit.jupiter.api.Test;

class ArmorMetadataB173Test {
    @Test void everySlotAndTierRetainsBetaConstructorState() {
        int[] mark = ContentB173.mark();
        try {
            int[] protection = {3, 8, 6, 3};
            int[] baseDurability = {33, 48, 45, 39};
            for (int tier = 0; tier < 5; tier++) {
                for (int slot = 0; slot < 4; slot++) {
                    var armor = new ArmorItem(30010 + tier * 4 + slot, tier, 7, slot);
                    assertEquals(tier, armor.type);
                    assertEquals(slot, armor.equipmentSlot);
                    assertEquals(7, armor.textureIndex);
                    assertEquals(protection[slot], armor.maxProtection);
                    assertEquals(baseDurability[slot] * (1 << tier), armor.getMaxDamage());
                    assertEquals(1, armor.getMaxCount());
                    assertTrue(armor.isDamageable());
                }
            }
        } finally {
            ContentB173.release(mark);
        }
    }

    @Test void modLoaderReflectionFindsTheOriginalArmorFieldByIndexAndName() throws Exception {
        int[] mark = ContentB173.mark();
        try {
            var armor = new ArmorItem(30040, 2, 7, 1);
            assertEquals("maxProtection", ReflectionB173.field(ArmorItem.class, 4).getName());
            assertEquals(8, ModLoader.getPrivateValue(ArmorItem.class, armor, 4));
            ModLoader.setPrivateValue(ArmorItem.class, armor, 4, 0);
            assertEquals(0, armor.maxProtection);
            assertEquals(0, ModLoader.getPrivateValue(ArmorItem.class, armor, "bl"));
            ModLoader.setPrivateValue(ArmorItem.class, armor, "maxProtection", 6);
            assertEquals(6, armor.maxProtection);
            assertEquals(2, armor.type);
            assertEquals(1, armor.equipmentSlot);
            assertEquals(7, armor.textureIndex);
            assertEquals(192, armor.getMaxDamage());
        } finally {
            ContentB173.release(mark);
        }
    }
}
