package legacyconnector.v1_12_2.net.minecraft.enchantment;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;

/** 1.12.2's protection enchantments (protection, fire, fall, blast, projectile) and their helpers for burning time and knockback. */
public class EnchantmentProtection extends Enchantment {
    public final Type protectionType;

    public EnchantmentProtection(Enchantment.Rarity rarityIn, Type protectionTypeIn, EntityEquipmentSlot... slots) {
        super(rarityIn, EnumEnchantmentType.ARMOR, slots);
        this.protectionType = protectionTypeIn;
        if (protectionTypeIn == Type.FALL) this.type = EnumEnchantmentType.ARMOR_FEET;
    }

    @Override
    public int getMinEnchantability(int level) {
        return protectionType.getMinimalEnchantability() + (level - 1) * protectionType.getEnchantIncreasePerLevel();
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + protectionType.getEnchantIncreasePerLevel();
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public int calcModifierDamage(int level, DamageSource source) {
        if (source.canHarmInCreative()) return 0;
        if (protectionType == Type.ALL) return level;
        if (protectionType == Type.FIRE && source.isFireDamage()) return level * 2;
        if (protectionType == Type.FALL && "fall".equals(source.getDamageType())) return level * 3;
        if (protectionType == Type.EXPLOSION && source.isExplosion()) return level * 2;
        return protectionType == Type.PROJECTILE && source.isProjectile() ? level * 2 : 0;
    }

    @Override
    public String getName() {
        return "enchantment.protect." + protectionType.getTypeName();
    }

    @Override
    public boolean canApplyTogether(Enchantment ench) {
        if (ench instanceof EnchantmentProtection other) {
            if (protectionType == other.protectionType) return false;
            return protectionType == Type.FALL || other.protectionType == Type.FALL;
        }
        return super.canApplyTogether(ench);
    }

    /** Burning time left after the entity's Fire Protection (15% less per level). */
    public static int getFireTimeForEntity(EntityLivingBase entity, int ticks) {
        int level = EnchantmentHelper.getMaxEnchantmentLevel(Enchantment.getEnchantmentByLocation("fire_protection"), entity);
        if (level > 0) ticks -= legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.floor((float) ticks * (float) level * 0.15F);
        return ticks;
    }

    /** Explosion knockback left after the entity's Blast Protection (15% less per level). */
    public static double getBlastDamageReduction(EntityLivingBase entity, double damage) {
        int level = EnchantmentHelper.getMaxEnchantmentLevel(Enchantment.getEnchantmentByLocation("blast_protection"), entity);
        if (level > 0) damage -= (double) legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.floor(damage * (double) ((float) level * 0.15F));
        return damage;
    }

    public enum Type {
        ALL("all", 1, 11, 20),
        FIRE("fire", 10, 8, 12),
        FALL("fall", 5, 6, 10),
        EXPLOSION("explosion", 5, 8, 12),
        PROJECTILE("projectile", 3, 6, 15);

        private final String typeName;
        private final int minEnchantability;
        private final int levelCost;
        private final int levelCostSpan;

        Type(String name, int minimal, int perLevel, int span) {
            this.typeName = name;
            this.minEnchantability = minimal;
            this.levelCost = perLevel;
            this.levelCostSpan = span;
        }

        public String getTypeName() {
            return typeName;
        }

        public int getMinimalEnchantability() {
            return minEnchantability;
        }

        public int getEnchantIncreasePerLevel() {
            return levelCost;
        }
    }
}
