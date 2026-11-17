package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/** Coremod hook site (generic, {@code CoremodSites1122.SHULKER_INSERT}), the hopper side: see {@code ShulkerBoxInsertHookMixin}. */
@CoremodHook
@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxEntityInsertHookMixin {
    @Inject(method = "canPlaceItemThroughFace", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodHopper(int slot, ItemStack stack, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (!CoremodSites1122.SHULKER_INSERT.listed()) return;
        boolean value = cir.getReturnValueZ();
        boolean changed = CoremodSites1122.shulkerAllows(stack, value);
        if (changed != value) cir.setReturnValue(changed);
    }
}
