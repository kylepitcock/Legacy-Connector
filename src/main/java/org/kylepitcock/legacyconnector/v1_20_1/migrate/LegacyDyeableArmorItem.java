package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

/** Stand-in for 1.20.1's {@code DyeableArmorItem}: armour whose colour is the {@code dyed_color} component ({@link LegacyDyeableLeatherItem}). */
public class LegacyDyeableArmorItem extends ArmorItem implements LegacyDyeableLeatherItem {
    public LegacyDyeableArmorItem(Holder<ArmorMaterial> material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }
}
