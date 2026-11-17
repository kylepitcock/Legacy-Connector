package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.world.entity.EquipmentSlot;

/** 1.20.1's {@code ArrowPiercingEnchantment} (piercing) over {@link LegacyEnchantment}: the costs and level of the original. */
public class LegacyArrowPiercingEnchantment extends LegacyEnchantment {
    public LegacyArrowPiercingEnchantment(Rarity rarity, EquipmentSlot... slots) {
        super(rarity, LegacyEnchantmentCategory.CROSSBOW, slots);
    }

    @Override
    public int getMinCost(int level) {
        return 1 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }
}
