package legacyconnector.v1_7_10.net.minecraft.enchantment;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;

/**
 * Protection enchantments (protection, fire, fall, blast, projectile): armour enchantments mods subclass for their own
 * damage types (Chocolate Quest's EnchantmentMagicDefense extends it), so it must be an {@link Enchantment}.
 */
public class EnchantmentProtection extends Enchantment {
    public static final String[] protectionName = {"all", "fire", "fall", "explosion", "projectile"};
    private static final int[] baseEnchantability = {1, 10, 5, 5, 3};
    private static final int[] levelEnchantability = {11, 8, 6, 8, 6};
    private static final int[] thresholdEnchantability = {20, 12, 10, 12, 15};

    /** 0 all, 1 fire, 2 fall, 3 explosion, 4 projectile. */
    public final int protectionType;

    public EnchantmentProtection(int id, int weight, int protectionType) {
        super(id, weight, EnumEnchantmentType.armor);
        this.protectionType = protectionType;
        if (protectionType == 2) this.type = EnumEnchantmentType.armor_feet;
    }

    @Override
    public int getMinEnchantability(int level) {
        int t = clampType();
        return baseEnchantability[t] + (level - 1) * levelEnchantability[t];
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + thresholdEnchantability[clampType()];
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    private int clampType() {
        return protectionType < 0 || protectionType >= baseEnchantability.length ? 0 : protectionType;
    }

    @Override
    public int calcModifierDamage(int level, DamageSource source) {
        if (source == null || source.canHarmInCreative()) return 0;
        float f = (6 + level * level) / 3.0F;
        switch (protectionType) {
            case 0: return (int) (f * 0.75F);
            case 1: return source.isFireDamage() ? (int) (f * 1.25F) : 0;
            case 2: return source == DamageSource.fall ? (int) (f * 2.5F) : 0;
            case 3: return source.isExplosion() ? (int) (f * 1.5F) : 0;
            case 4: return source.isProjectile() ? (int) (f * 1.5F) : 0;
            default: return 0;
        }
    }

    @Override
    public boolean canApplyTogether(Enchantment other) {
        if (other instanceof EnchantmentProtection) {
            EnchantmentProtection o = (EnchantmentProtection) other;
            if (o.protectionType == protectionType) return false;
            return protectionType == 2 || o.protectionType == 2;
        }
        return super.canApplyTogether(other);
    }

    /** Reduces explosion knockback by the blast protection of the entity's armour (no armour enchantments are read: unchanged). */
    public static double func_92092_a(Entity entity, double knockback) {
        return knockback;
    }
}
