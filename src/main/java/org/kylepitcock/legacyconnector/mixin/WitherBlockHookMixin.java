package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.WITHER_BLOCK}): 1.12.2 coremods put a hook first in {@code EntityWither.canDestroyBlock} that answers
 * the block the check goes on with (a block it wants kept answers an indestructible one). A no-op without such a hook.
 */
@CoremodHook
@Mixin(WitherBoss.class)
public abstract class WitherBlockHookMixin {
    @Inject(method = "canDestroy", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacyconnector$coremodWitherBlock(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!CoremodSites1122.WITHER_BLOCK.listed()) return;
        BlockState changed = CoremodSites1122.witherBlock(state);
        if (changed != state) cir.setReturnValue(!changed.isAir() && !changed.is(BlockTags.WITHER_IMMUNE));
    }
}
