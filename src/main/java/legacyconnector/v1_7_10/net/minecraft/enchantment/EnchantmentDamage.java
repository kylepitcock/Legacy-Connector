package legacyconnector.v1_7_10.net.minecraft.enchantment;

import legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureAttribute;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/**
 * 1.7.10's melee damage enchantments (type 0 sharpness, 1 smite, 2 bane of arthropods), a subclass of Enchantment mods extend for
 * their own damage enchantments (The Lion King's Hyena enchantments pass type 0 and override the damage bonus).
 */
public class EnchantmentDamage extends Enchantment {
    private static final String[] PROTECTION_NAME = {"all", "undead", "arthropods"};
    private static final int[] BASE_ENCHANTABILITY = {1, 5, 5};
    private static final int[] LEVEL_ENCHANTABILITY = {11, 8, 8};
    private static final int[] THRESHOLD_ENCHANTABILITY = {20, 20, 20};
    public final int damageType;

    public EnchantmentDamage(int id, int weight, int damageType) {
        super(id, weight, EnumEnchantmentType.weapon);
        this.damageType = damageType;
    }

    private int index() {
        return Math.max(0, Math.min(2, damageType));
    }

    @Override
    public int getMinEnchantability(int level) {
        return BASE_ENCHANTABILITY[index()] + (level - 1) * LEVEL_ENCHANTABILITY[index()];
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + THRESHOLD_ENCHANTABILITY[index()];
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public float func_152376_a(int level, EnumCreatureAttribute attribute) {
        if (damageType == 0) return level * 1.25F;
        if (damageType == 1 && attribute == EnumCreatureAttribute.UNDEAD) return level * 2.5F;
        if (damageType == 2 && attribute == EnumCreatureAttribute.ARTHROPOD) return level * 2.5F;
        return 0.0F;
    }

    @Override
    public String getName() {
        return "enchantment.damage." + PROTECTION_NAME[index()];
    }

    @Override
    public boolean canApplyTogether(Enchantment other) {
        return !(other instanceof EnchantmentDamage);
    }

    @Override
    public boolean canApply(ItemStack stack) {
        return stack != null && stack.getItem() instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemAxe || super.canApply(stack);
    }
}
