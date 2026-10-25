package legacyconnector.v1_12_2.net.minecraft.enchantment;

public enum EnumEnchantmentType {
    ALL, ARMOR, ARMOR_FEET, ARMOR_LEGS, ARMOR_CHEST, ARMOR_HEAD, WEAPON, DIGGER, FISHING_ROD, BREAKABLE, BOW, WEARABLE, VANISHABLE;

    public boolean canEnchantItem(legacyconnector.v1_12_2.net.minecraft.item.Item item) {
        return true;
    }
}
