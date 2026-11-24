package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.world.entity.EquipmentSlot;

/** 1.20.1's {@code TridentLoyaltyEnchantment} over {@link LegacyEnchantment}: the costs and maximum level of the original; the effect is the data-driven vanilla enchantment's. */
public class LegacyTridentLoyaltyEnchantment extends LegacyEnchantment {
    public LegacyTridentLoyaltyEnchantment(Rarity rarity, EquipmentSlot... slots) {
        super(rarity, LegacyEnchantmentCategory.TRIDENT, slots);
    }

    @Override
    public int getMinCost(int level) {
        return 12 + level * 7;
    }

    @Override
    public int getMaxCost(int level) {
        return 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}
