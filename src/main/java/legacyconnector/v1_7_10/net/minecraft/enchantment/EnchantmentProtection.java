package legacyconnector.v1_7_10.net.minecraft.enchantment;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;

/**
 * 1.7.10's protection enchantments (type 0 all, 1 fire, 2 fall, 3 explosion, 4 projectile), a subclass of Enchantment mods extend
 * (Chocolate Quest's EnchantmentMagicDefense assigns its instance to an Enchantment field: with the old plain class the mod failed
 * verification during construction).
 */
public class EnchantmentProtection extends Enchantment {
    private static final String[] PROTECTION_NAME = {"all", "fire", "fall", "explosion", "projectile"};
    private static final int[] BASE_ENCHANTABILITY = {1, 10, 5, 5, 3};
    private static final int[] LEVEL_ENCHANTABILITY = {11, 8, 6, 8, 6};
    private static final int[] THRESHOLD_ENCHANTABILITY = {20, 12, 10, 12, 15};
    public final int protectionType;

    public EnchantmentProtection(int id, int weight, int protectionType) {
        super(id, weight, EnumEnchantmentType.armor);
        this.protectionType = protectionType;
        if (protectionType == 2) this.type = EnumEnchantmentType.armor_feet;
        if (protectionType >= 0 && protectionType < PROTECTION_NAME.length) setName("protect." + PROTECTION_NAME[protectionType]);
    }

    @Override
    public int getMinEnchantability(int level) {
        return BASE_ENCHANTABILITY[protectionType] + (level - 1) * LEVEL_ENCHANTABILITY[protectionType];
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + THRESHOLD_ENCHANTABILITY[protectionType];
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public int calcModifierDamage(int level, DamageSource source) {
        if (source.canHarmInCreative()) return 0;
        float f = (6 + level * level) / 3.0F;
        if (protectionType == 0) return MathHelper.floor_float(f * 0.75F);
        if (protectionType == 1 && source.isFireDamage()) return MathHelper.floor_float(f * 1.25F);
        if (protectionType == 2 && source == DamageSource.fall) return MathHelper.floor_float(f * 2.5F);
        if (protectionType == 3 && source.isExplosion()) return MathHelper.floor_float(f * 1.5F);
        if (protectionType == 4 && source.isProjectile()) return MathHelper.floor_float(f * 1.5F);
        return 0;
    }

    @Override
    public boolean canApplyTogether(Enchantment other) {
        if (other instanceof EnchantmentProtection p) {
            if (p.protectionType == protectionType) return false;
            return protectionType == 2 || p.protectionType == 2;
        }
        return super.canApplyTogether(other);
    }

    /** Reduces explosion knockback by the blast protection of the entity's armour (no armour enchantments are read: unchanged). */
    public static double func_92092_a(Entity entity, double knockback) {
        return knockback;
    }
}
