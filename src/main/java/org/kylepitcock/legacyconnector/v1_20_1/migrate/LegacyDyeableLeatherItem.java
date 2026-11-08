package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * Stands in for {@code net.minecraft.world.item.DyeableLeatherItem} (1.20.1): the colour of an item is the {@code dyed_color} component in 1.21.1. The default
 * colour of a stack without one is leather brown, as before.
 */
public interface LegacyDyeableLeatherItem {
    default boolean hasCustomColor(ItemStack stack) {
        return stack.has(DataComponents.DYED_COLOR);
    }

    default int getColor(ItemStack stack) {
        DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
        return dyed != null ? dyed.rgb() : 10511680;
    }

    default void clearColor(ItemStack stack) {
        stack.remove(DataComponents.DYED_COLOR);
    }

    default void setColor(ItemStack stack, int color) {
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color, true));
    }
}
