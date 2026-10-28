package legacyconnector.v1_12_2.net.minecraft.enchantment;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** Unbreaking: a chance per use that the stack takes no damage. */
public class EnchantmentDurability extends Enchantment {
    protected EnchantmentDurability(Enchantment.Rarity rarityIn, EntityEquipmentSlot... slots) {
        super(rarityIn, EnumEnchantmentType.BREAKABLE, slots);
        setName("durability");
    }

    @Override
    public int getMinEnchantability(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return super.getMinEnchantability(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean canApply(ItemStack stack) {
        return stack.isItemStackDamageable() || super.canApply(stack);
    }

    /** True when this use should not damage the stack (armour keeps 60% of its wear regardless). */
    public static boolean negateDamage(ItemStack stack, int level, Random rand) {
        if (stack.getItem() instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemArmor && rand.nextFloat() < 0.6F) return false;
        return rand.nextInt(level + 1) > 0;
    }
}
