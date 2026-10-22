package mccompat.v1_7_10.net.minecraft.enchantment;

import mccompat.v1_7_10.net.minecraft.util.WeightedRandom;

/** 1.7.10 {@code net.minecraft.enchantment.EnchantmentData}: an enchantment with a level. */
public class EnchantmentData extends WeightedRandom.Item {
    public final Enchantment enchantmentobj;
    public final int enchantmentLevel;

    public EnchantmentData(Enchantment enchantment, int level) {
        super(enchantment.getWeight());
        this.enchantmentobj = enchantment;
        this.enchantmentLevel = level;
    }
}
