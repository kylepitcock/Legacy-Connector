package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.SHULKER_INSERT}): 1.12.2 coremods replaced the "no shulker box in a shulker box" check of the slot (and
 * of the hopper side, {@code ShulkerBoxEntityInsertHookMixin}) by a hook that decides what a shulker box takes. A no-op without such a hook.
 */
@Mixin(ShulkerBoxSlot.class)
public abstract class ShulkerBoxInsertHookMixin {
    @Inject(method = "mayPlace", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodSlot(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!CoremodSites1122.SHULKER_INSERT.listed()) return;
        boolean value = cir.getReturnValueZ();
        boolean changed = CoremodSites1122.shulkerAllows(stack, value);
        if (changed != value) cir.setReturnValue(changed);
    }
}
