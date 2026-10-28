package legacyconnector.v1_12_2.net.minecraft.enchantment;

import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;

/** Looting, Fortune and Luck of the Sea: more drops (not together with Silk Touch). */
public class EnchantmentLootBonus extends Enchantment {
    protected EnchantmentLootBonus(Enchantment.Rarity rarityIn, EnumEnchantmentType typeIn, EntityEquipmentSlot... slots) {
        super(rarityIn, typeIn, slots);
        if (typeIn == EnumEnchantmentType.DIGGER) setName("lootBonusDigger");
        else if (typeIn == EnumEnchantmentType.FISHING_ROD) setName("lootBonusFishing");
        else setName("lootBonus");
    }

    @Override
    public int getMinEnchantability(int level) {
        return 15 + (level - 1) * 9;
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
    public boolean canApplyTogether(Enchantment ench) {
        var name = ench == null ? null : ench.getRegistryName();
        boolean silk = name != null && "minecraft".equals(name.getNamespace()) && "silk_touch".equals(name.getPath());
        return super.canApplyTogether(ench) && !silk;
    }
}
