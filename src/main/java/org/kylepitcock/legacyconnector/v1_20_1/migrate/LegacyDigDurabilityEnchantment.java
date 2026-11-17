package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.world.entity.EquipmentSlot;

/** 1.20.1's {@code DigDurabilityEnchantment} (unbreaking) over {@link LegacyEnchantment}: the costs and level of the original; the saving of durability itself is the data-driven vanilla enchantment's. */
public class LegacyDigDurabilityEnchantment extends LegacyEnchantment {
    public LegacyDigDurabilityEnchantment(Rarity rarity, EquipmentSlot... slots) {
        super(rarity, LegacyEnchantmentCategory.BREAKABLE, slots);
    }

    @Override
    public int getMinCost(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxCost(int level) {
        return super.getMinCost(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}
