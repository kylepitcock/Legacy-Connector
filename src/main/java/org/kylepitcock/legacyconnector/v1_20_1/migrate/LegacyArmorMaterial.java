package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Stands in for {@code net.minecraft.world.item.ArmorMaterial} of 1.20.1, an interface a mod implemented (usually with an enum). In 1.21.1 it is a final record
 * held in a registry; {@link ArmorCompat} builds that record from an implementation of this interface.
 */
public interface LegacyArmorMaterial {
    int getDurabilityForType(ArmorItem.Type type);

    int getDefenseForType(ArmorItem.Type type);

    int getEnchantmentValue();

    SoundEvent getEquipSound();

    Ingredient getRepairIngredient();

    String getName();

    float getToughness();

    float getKnockbackResistance();
}
