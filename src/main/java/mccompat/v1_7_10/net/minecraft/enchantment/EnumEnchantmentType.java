package mccompat.v1_7_10.net.minecraft.enchantment;

import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemArmor;
import mccompat.v1_7_10.net.minecraft.item.ItemSword;
import mccompat.v1_7_10.net.minecraft.item.ItemTool;

public enum EnumEnchantmentType {
    all, armor, armor_feet, armor_legs, armor_torso, armor_head, weapon, digger, fishing_rod, breakable, bow;

    public boolean canEnchantItem(Item item) {
        if (this == all) return true;
        if (this == breakable) return item.isDamageable();
        if (item instanceof ItemArmor piece) {
            if (this == EnumEnchantmentType.armor) return true;
            int slot = piece.armorType;
            return slot == 0 ? this == armor_head : slot == 1 ? this == armor_torso : slot == 2 ? this == armor_legs : slot == 3 && this == armor_feet;
        }
        if (item instanceof ItemSword) return this == weapon;
        if (item instanceof ItemTool) return this == digger;
        return false;
    }
}
