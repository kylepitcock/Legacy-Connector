package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;

/** 1.20.1's {@code ProtectionEnchantment} (gone: protection is data driven in 1.21.1) over {@link LegacyEnchantment}, with the same costs and damage reduction. */
public class LegacyProtectionEnchantment extends LegacyEnchantment {
    public enum Type {
        ALL(1, 11), FIRE(10, 8), FALL(5, 6), EXPLOSION(5, 8), PROJECTILE(3, 6);

        private final int minCost;
        private final int levelCost;

        Type(int minCost, int levelCost) {
            this.minCost = minCost;
            this.levelCost = levelCost;
        }

        public int getMinCost() {
            return minCost;
        }

        public int getLevelCost() {
            return levelCost;
        }
    }

    public final Type type;

    public LegacyProtectionEnchantment(Rarity rarity, Type type, EquipmentSlot... slots) {
        super(rarity, type == Type.FALL ? LegacyEnchantmentCategory.ARMOR_FEET : LegacyEnchantmentCategory.ARMOR, slots);
        this.type = type;
    }

    @Override
    public int getMinCost(int level) {
        return type.getMinCost() + (level - 1) * type.getLevelCost();
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + type.getLevelCost();
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public int getDamageProtection(int level, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return 0;
        return switch (type) {
            case ALL -> level;
            case FIRE -> source.is(DamageTypeTags.IS_FIRE) ? level * 2 : 0;
            case FALL -> source.is(DamageTypeTags.IS_FALL) ? level * 3 : 0;
            case EXPLOSION -> source.is(DamageTypeTags.IS_EXPLOSION) ? level * 2 : 0;
            case PROJECTILE -> source.is(DamageTypeTags.IS_PROJECTILE) ? level * 2 : 0;
        };
    }

    @Override
    protected boolean checkCompatibility(LegacyEnchantment other) {
        if (other instanceof LegacyProtectionEnchantment p) {
            return type != p.type && (type == Type.FALL || p.type == Type.FALL);
        }
        return super.checkCompatibility(other);
    }
}
