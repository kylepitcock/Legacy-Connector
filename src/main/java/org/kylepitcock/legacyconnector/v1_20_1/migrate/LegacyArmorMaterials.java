package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.crafting.Ingredient;

/** Stands in for the vanilla {@code ArmorMaterials} enum of 1.20.1: each constant wraps the 1.21.1 material of the same name. */
public final class LegacyArmorMaterials implements LegacyArmorMaterial {
    public static final LegacyArmorMaterials LEATHER = new LegacyArmorMaterials("leather", ArmorMaterials.LEATHER);
    public static final LegacyArmorMaterials CHAIN = new LegacyArmorMaterials("chainmail", ArmorMaterials.CHAIN);
    public static final LegacyArmorMaterials IRON = new LegacyArmorMaterials("iron", ArmorMaterials.IRON);
    public static final LegacyArmorMaterials GOLD = new LegacyArmorMaterials("gold", ArmorMaterials.GOLD);
    public static final LegacyArmorMaterials DIAMOND = new LegacyArmorMaterials("diamond", ArmorMaterials.DIAMOND);
    public static final LegacyArmorMaterials TURTLE = new LegacyArmorMaterials("turtle", ArmorMaterials.TURTLE);
    public static final LegacyArmorMaterials NETHERITE = new LegacyArmorMaterials("netherite", ArmorMaterials.NETHERITE);

    private static final int[] BASE_DURABILITY = {11, 16, 16, 13};

    final String name;
    final Holder<ArmorMaterial> holder;

    private LegacyArmorMaterials(String name, Holder<ArmorMaterial> holder) {
        this.name = name;
        this.holder = holder;
    }

    /** The multiplier vanilla used per material (the 1.21.1 record keeps no durability). */
    private int multiplier() {
        return switch (name) {
            case "leather" -> 5;
            case "chainmail" -> 15;
            case "iron" -> 15;
            case "gold" -> 7;
            case "diamond" -> 33;
            case "turtle" -> 25;
            default -> 37;
        };
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY[Math.min(3, type.ordinal())] * multiplier();
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return holder.value().getDefense(type);
    }

    @Override
    public int getEnchantmentValue() {
        return holder.value().enchantmentValue();
    }

    @Override
    public SoundEvent getEquipSound() {
        return holder.value().equipSound().value();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return holder.value().repairIngredient().get();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getToughness() {
        return holder.value().toughness();
    }

    @Override
    public float getKnockbackResistance() {
        return holder.value().knockbackResistance();
    }

    public static LegacyArmorMaterials[] values() {
        return new LegacyArmorMaterials[] {LEATHER, CHAIN, IRON, GOLD, DIAMOND, TURTLE, NETHERITE};
    }

    @Override
    public String toString() {
        return name;
    }
}
