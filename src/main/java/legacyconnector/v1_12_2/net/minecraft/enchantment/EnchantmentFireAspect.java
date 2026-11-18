package legacyconnector.v1_12_2.net.minecraft.enchantment;

import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;

/** 1.12.2 fire: levels 1-2, found on weapons. */
public class EnchantmentFireAspect extends Enchantment {
    protected EnchantmentFireAspect(Enchantment.Rarity rarityIn, EntityEquipmentSlot... slots) {
        super(rarityIn, EnumEnchantmentType.WEAPON, slots);
        setName("fire");
    }

    @Override
    public int getMinEnchantability(int enchantmentLevel) {
        return 10 + 20 * (enchantmentLevel - 1);
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
