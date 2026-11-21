package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Stand-in for 1.20.1's {@code SimpleFoiledItem} (gone in 1.21.1): an item that always shows the enchantment glint. */
public class LegacySimpleFoiledItem extends Item {
    public LegacySimpleFoiledItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
