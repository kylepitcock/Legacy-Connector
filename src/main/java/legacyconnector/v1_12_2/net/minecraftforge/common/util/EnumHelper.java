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

    public static legacyconnector.v1_12_2.net.minecraft.item.EnumRarity addRarity(String name, legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting color, String displayName) {
        return new legacyconnector.v1_12_2.net.minecraft.item.EnumRarity(name, color, displayName);
    }

    public static legacyconnector.v1_12_2.net.minecraft.enchantment.EnumEnchantmentType addEnchantmentType(String name, com.google.common.base.Predicate<Item> test) {
        return new legacyconnector.v1_12_2.net.minecraft.enchantment.EnumEnchantmentType(name, test);
    }

    /**
     * A new creature type (a spawn group with its own cap): {@code creatureClass} is what counts toward the cap. The modern game spawns
     * by its own categories, so creatures of an added type are counted as the type's legacy class says.
     */
    public static legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType addCreatureType(String name, Class<?> typeClass, int maxNumber,
            legacyconnector.v1_12_2.net.minecraft.block.material.Material material, boolean peaceful, boolean animal) {
        var type = org.kylepitcock.legacyconnector.core.LegacyEnums.add(legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType.class, name,
                new Class<?>[] {int.class, boolean.class, boolean.class}, new Object[] {maxNumber, peaceful, animal});
        legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType.mcc$setCreatureClass(type, typeClass);
        return type;
    }

    /** Adds a constant to a real enum (a synthesized or the mod's own); parameter types inferred from the values. */
    public static <T extends Enum<?>> T addEnum(Class<T> enumType, String enumName, Object... paramValues) {
        return org.kylepitcock.legacyconnector.core.LegacyEnums.add(enumType, enumName, paramValues);
    }

    public static <T extends Enum<?>> T addEnum(Class<?>[][] map, Class<T> enumType, String enumName, Object... paramValues) {
        return org.kylepitcock.legacyconnector.core.LegacyEnums.add(map, enumType, enumName, paramValues);
    }

    public static <T extends Enum<?>> T addEnum(Class<T> enumType, String enumName, Class<?>[] paramTypes, Object... paramValues) {
        return org.kylepitcock.legacyconnector.core.LegacyEnums.add(enumType, enumName, paramTypes, paramValues);
    }

    public static void setFailsafeFieldValue(java.lang.reflect.Field field, Object target, Object value) throws Exception {
        org.kylepitcock.legacyconnector.core.LegacyEnums.setFailsafeFieldValue(field, target, value);
    }
}
