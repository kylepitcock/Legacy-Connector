package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Coremod hook site (generic, {@code CoremodSites1122.FURNACE_INPUT}): a hook of a 1.12.2 coremod keeps stacks out of the furnace's input slot. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceInputHookMixin {
    @Inject(method = "canPlaceItem", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodFurnaceInput(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (slot == 0 && CoremodSites1122.FURNACE_INPUT.listed() && cir.getReturnValueZ() && !CoremodSites1122.furnaceInput(stack, true)) cir.setReturnValue(false);
    }
}
