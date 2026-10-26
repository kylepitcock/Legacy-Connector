package legacyconnector.v1_12_2.net.minecraft.enchantment;

/** An enchantment at a level, weighted by its rarity (enchanting table and loot rolls). */
public class EnchantmentData extends legacyconnector.v1_12_2.net.minecraft.util.WeightedRandom.Item {
    public final Enchantment enchantment;
    public final int enchantmentLevel;

    public EnchantmentData(Enchantment enchantment, int level) {
        super(enchantment.getRarity().getWeight());
        this.enchantment = enchantment;
        this.enchantmentLevel = level;
    }
}
