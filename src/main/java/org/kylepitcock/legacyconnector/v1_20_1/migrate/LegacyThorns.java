package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.util.RandomSource;

/**
 * Stands in for the static helpers of {@code ThornsEnchantment} (1.20.1), which is data in 1.21.1 (an entity effect component): the chance and the damage
 * of a thorns hit as vanilla 1.20.1 computed them, for mods that run their own thorns-like enchantments with the same numbers.
 */
public final class LegacyThorns {
    private LegacyThorns() {}

    /** 15 % per enchantment level. */
    public static boolean shouldHit(int level, RandomSource random) {
        return level > 0 && random.nextFloat() < 0.15F * level;
    }

    /** Above level 10 the level minus 10, else 1 to 4 damage. */
    public static int getDamage(int level, RandomSource random) {
        return level > 10 ? level - 10 : 1 + random.nextInt(4);
    }
}
