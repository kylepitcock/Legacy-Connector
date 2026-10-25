package legacyconnector.v1_12_2.net.minecraftforge.common.util;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemArmor;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;

/** Forge added enum constants by bytecode tricks; the tiers the layer needs are plain classes, so they are simply created. */
public class EnumHelper {
    public static ItemArmor.ArmorMaterial addArmorMaterial(String name, String textureName, int durability, int[] reductionAmounts, int enchantability,
            SoundEvent soundOnEquip, float toughness) {
        return new ItemArmor.ArmorMaterial(name, durability, reductionAmounts, enchantability, soundOnEquip, toughness);
    }

    public static Item.ToolMaterial addToolMaterial(String name, int harvestLevel, int maxUses, float efficiency, float damage, int enchantability) {
        return new Item.ToolMaterial(name, harvestLevel, maxUses, efficiency, damage, enchantability);
    }
}
