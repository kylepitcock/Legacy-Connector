package legacyconnector.v1_12_2.net.minecraft.util;

/** 1.12.2's armour and enchantment damage reduction formulas. */
public class CombatRules {
    public static float getDamageAfterAbsorb(float damage, float totalArmor, float toughnessAttribute) {
        float f = 2.0F + toughnessAttribute / 4.0F;
        float f1 = legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.clamp(totalArmor - damage / f, totalArmor * 0.2F, 20.0F);
        return damage * (1.0F - f1 / 25.0F);
    }

    public static float getDamageAfterMagicAbsorb(float damage, float enchantModifiers) {
        float f = legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.clamp(enchantModifiers, 0.0F, 20.0F);
        return damage * (1.0F - f / 25.0F);
    }
}
