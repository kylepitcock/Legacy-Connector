package legacyconnector.v1_7_10.net.minecraftforge.common.util;

import java.lang.reflect.Field;
import legacyconnector.v1_7_10.net.minecraft.item.EnumRarity;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemArmor;
import legacyconnector.v1_7_10.net.minecraft.util.EnumChatFormatting;
import org.kylepitcock.legacyconnector.core.LegacyEnums;

/** Forge's runtime enum extension; see {@link LegacyEnums}. */
public class EnumHelper {
    private EnumHelper() {}

    public static Item.ToolMaterial addToolMaterial(String name, int harvestLevel, int maxUses, float efficiency, float damage, int enchantability) {
        return addEnum(Item.ToolMaterial.class, name, new Class<?>[] {int.class, int.class, float.class, float.class, int.class},
                new Object[] {harvestLevel, maxUses, efficiency, damage, enchantability});
    }

    public static ItemArmor.ArmorMaterial addArmorMaterial(String name, int durability, int[] reductionAmounts, int enchantability) {
        return addEnum(ItemArmor.ArmorMaterial.class, name, new Class<?>[] {int.class, int[].class, int.class},
                new Object[] {durability, reductionAmounts, enchantability});
    }

    public static legacyconnector.v1_7_10.net.minecraft.enchantment.EnumEnchantmentType addEnchantmentType(String name) {
        return addEnum(legacyconnector.v1_7_10.net.minecraft.enchantment.EnumEnchantmentType.class, name);
    }

    public static EnumRarity addRarity(String name, EnumChatFormatting color, String displayName) {
        return addEnum(EnumRarity.class, name, new Object[] {color, displayName});
    }

    /** Parameter types inferred from the values (boxed types matched against primitive constructor parameters). */
    public static <T extends Enum<?>> T addEnum(Class<T> enumType, String enumName, Object... paramValues) {
        return LegacyEnums.add(enumType, enumName, paramValues);
    }

    public static <T extends Enum<?>> T addEnum(Class<?>[][] map, Class<T> enumType, String enumName, Object... paramValues) {
        return LegacyEnums.add(map, enumType, enumName, paramValues);
    }

    public static <T extends Enum<?>> T addEnum(Class<T> enumType, String enumName, Class<?>[] paramTypes, Object[] paramValues) {
        return LegacyEnums.add(enumType, enumName, paramTypes, paramValues);
    }

    public static void setFailsafeFieldValue(Field field, Object target, Object value) throws Exception {
        LegacyEnums.setFailsafeFieldValue(field, target, value);
    }
}
