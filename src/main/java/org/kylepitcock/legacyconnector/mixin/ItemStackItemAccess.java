package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.7.10 stacks could change their item in place ({@code ItemStack.setItem}, and a damage that selects another subtype), and mods
 * rely on it: IC2's electric item manager swaps a battery between its empty and charged item on the stack in the slot. A modern stack's
 * item and component map are final, so a legacy view of a stack someone else holds (a slot's) writes through these accessors.
 */
@Mixin(ItemStack.class)
public interface ItemStackItemAccess {
    @Mutable @Accessor("item") void legacyconnector$setItem(Item item);

    @Mutable @Accessor("components") void legacyconnector$setComponents(PatchedDataComponentMap components);
}
