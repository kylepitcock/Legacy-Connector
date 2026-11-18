package legacyconnector.v1_12_2.net.minecraft.enchantment;

import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;

/** 1.12.2 knockback: levels 1-2, found on weapons. */
public class EnchantmentKnockback extends Enchantment {
    protected EnchantmentKnockback(Enchantment.Rarity rarityIn, EntityEquipmentSlot... slots) {
        super(rarityIn, EnumEnchantmentType.WEAPON, slots);
        setName("knockback");
    }

    @Override
    public int getMinEnchantability(int enchantmentLevel) {
        return 5 + 20 * (enchantmentLevel - 1);
    }

    @Override
    public int getMaxEnchantability(int enchantmentLevel) {
        return super.getMinEnchantability(enchantmentLevel) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }
}
