package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Coremod hook site (generic, {@code CoremodSites.FURNACE_TAKEN_OVER}): 1.7.10 coremods put a hook first in {@code TileEntityFurnace.updateEntity}; one
 * answering true leaves the furnace's own update out. A no-op without such a hook.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceTakenOverHookMixin {
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacyconnector$coremodFurnaceTakenOver(Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        if (CoremodSites.FURNACE_TAKEN_OVER.listed() && CoremodSites.furnaceTakenOver(level, pos)) ci.cancel();
    }
}
