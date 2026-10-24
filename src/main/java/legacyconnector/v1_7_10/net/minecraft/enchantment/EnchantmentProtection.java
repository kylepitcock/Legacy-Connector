package legacyconnector.v1_7_10.net.minecraft.enchantment;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

/** Protection enchantments: the helper explosions use to scale the knockback an armoured entity takes. */
public class EnchantmentProtection {
    /** Reduces explosion knockback by the blast protection of the entity's armour (no armour enchantments are read: unchanged). */
    public static double func_92092_a(Entity entity, double knockback) {
        return knockback;
    }
}
