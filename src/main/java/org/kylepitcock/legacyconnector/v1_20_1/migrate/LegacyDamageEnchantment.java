package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.world.entity.EquipmentSlot;

/** 1.20.1's {@code DamageEnchantment} (sharpness, smite, bane of arthropods) over {@link LegacyEnchantment}. Type 0 all, 1 undead, 2 arthropod. */
public class LegacyDamageEnchantment extends LegacyEnchantment {
    private static final int[] MIN_COST = {1, 5, 5};
    private static final int[] LEVEL_COST = {11, 8, 8};
    private static final int[] LEVEL_COST_SPAN = {20, 20, 20};

    public final int type;

    public LegacyDamageEnchantment(Rarity rarity, int type, EquipmentSlot... slots) {
        super(rarity, LegacyEnchantmentCategory.WEAPON, slots);
        this.type = type;
    }

    @Override
    public int getMinCost(int level) {
        return MIN_COST[type] + (level - 1) * LEVEL_COST[type];
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + LEVEL_COST_SPAN[type];
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public float getDamageBonus(int level, LegacyMobType mobType) {
        if (type == 0) return 1.0F + Math.max(0, level - 1) * 0.5F;
        if (type == 1 && mobType == LegacyMobType.UNDEAD) return level * 2.5F;
        return type == 2 && mobType == LegacyMobType.ARTHROPOD ? level * 2.5F : 0.0F;
    }

    @Override
    protected boolean checkCompatibility(LegacyEnchantment other) {
        return !(other instanceof LegacyDamageEnchantment) && super.checkCompatibility(other);
    }
}
