package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.world.entity.EquipmentSlot;

/** 1.20.1's {@code KnockbackEnchantment} over {@link LegacyEnchantment}: the costs and maximum level of the original; the effect is the data-driven vanilla enchantment's. */
public class LegacyKnockbackEnchantment extends LegacyEnchantment {
    public LegacyKnockbackEnchantment(Rarity rarity, EquipmentSlot... slots) {
        super(rarity, LegacyEnchantmentCategory.WEAPON, slots);
    }

    @Override
    public int getMinCost(int level) {
        return 5 + 25 * (level - 1);
    }

    @Override
    public int getMaxCost(int level) {
        return super.getMinCost(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }
}
