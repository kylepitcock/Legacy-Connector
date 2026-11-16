package legacyconnector.v1_6_4.net.minecraft.src.forge;

import legacyconnector.v1_7_10.net.minecraft.item.ItemArmor;
import legacyconnector.v1_7_10.net.minecraft.item.Item;

/** Forge 3.x's enum extension helper on the shim's. */
public class EnumHelper {
    private EnumHelper() {}

    public static Item.ToolMaterial addToolMaterial(String name, int harvestLevel, int maxUses, float efficiency, int damage, int enchantability) {
        return legacyconnector.v1_7_10.net.minecraftforge.common.util.EnumHelper.addToolMaterial(name, harvestLevel, maxUses, efficiency, damage, enchantability);
    }

    public static ItemArmor.ArmorMaterial addArmorMaterial(String name, int durability, int[] reductionAmounts, int enchantability) {
        return legacyconnector.v1_7_10.net.minecraftforge.common.util.EnumHelper.addArmorMaterial(name, durability, reductionAmounts, enchantability);
    }

    public static <T extends Enum<?>> T addEnum(boolean test, Class<T> enumType, String enumName, Class<?>[] paramTypes, Object[] paramValues) {
        return legacyconnector.v1_7_10.net.minecraftforge.common.util.EnumHelper.addEnum(enumType, enumName, paramTypes, paramValues);
    }

    public static <T extends Enum<?>> T addEnum(Class<T> enumType, String enumName, boolean test) {
        return legacyconnector.v1_7_10.net.minecraftforge.common.util.EnumHelper.addEnum(enumType, enumName);
    }
}
