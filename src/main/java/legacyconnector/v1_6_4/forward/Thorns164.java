package legacyconnector.v1_6_4.forward;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.enchantment.Enchantment;
import legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentHelper;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;

/**
 * 1.6.4 {@code EnchantmentThorns} statics (the 1.7.10 shim has no such class; the loader's call rules in forward/1.6.4/member-manual.tsv send the static calls
 * here). Mobs of old mods call {@link #attack} from their own attack code, as vanilla's players and mobs did in 1.6.4.
 */
public final class Thorns164 {
    private Thorns164() {}

    /** {@code EnchantmentThorns.func_92094_a(level, random)}: whether a hit against a wearer with this thorns level is reflected (15% per level). */
    public static boolean shouldHit(int level, Random rand) {
        return level > 0 && rand.nextFloat() < 0.15F * level;
    }

    /** {@code EnchantmentThorns.func_92095_b(level, random)}: the damage of a reflected hit (level - 10 from level 11, else 1 to 4). */
    public static int damage(int level, Random rand) {
        return level > 10 ? level - 10 : 1 + rand.nextInt(4);
    }

    /**
     * {@code EnchantmentThorns.func_92096_a(attacker, wearer, random)}: the attacker hurts itself on the thorns of the wearer's armor (the highest level
     * worn). A reflected hit damages the attacker and wears the piece by 3, a miss wears it by 1, as in 1.6.4.
     */
    public static void attack(Entity attacker, EntityLivingBase wearer, Random rand) {
        if (wearer == null) return;
        Enchantment thorns = Enchantment.thorns;
        ItemStack[] worn = wearer.getLastActiveItems();
        int level = 0;
        for (int slot = 1; slot < worn.length; slot++) level = Math.max(level, EnchantmentHelper.getEnchantmentLevel(thorns.effectId, worn[slot]));
        ItemStack piece = EnchantmentHelper.func_92099_a(thorns, wearer);
        if (shouldHit(level, rand)) {
            if (attacker != null) {
                attacker.attackEntityFrom(DamageSource.causeThornsDamage(wearer), damage(level, rand));
                attacker.playSound("damage.thorns", 0.5F, 1.0F);
            }
            if (piece != null) piece.damageItem(3, wearer);
        } else if (piece != null) {
            piece.damageItem(1, wearer);
        }
    }
}
