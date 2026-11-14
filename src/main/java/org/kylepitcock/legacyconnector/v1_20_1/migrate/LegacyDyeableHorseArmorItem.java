package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Stands in for 1.20.1's {@code DyeableHorseArmorItem}: horse armour whose colour is the {@code dyed_color} component, with an overlay texture. */
public class LegacyDyeableHorseArmorItem extends LegacyHorseArmorItem implements LegacyDyeableLeatherItem {
    public LegacyDyeableHorseArmorItem(int protection, String name, Item.Properties properties) {
        super(protection, ResourceLocation.withDefaultNamespace("textures/entity/horse/armor/horse_armor_" + name + ".png"), properties, true);
    }

    public LegacyDyeableHorseArmorItem(int protection, ResourceLocation texture, Item.Properties properties) {
        super(protection, texture, properties, true);
    }
}
