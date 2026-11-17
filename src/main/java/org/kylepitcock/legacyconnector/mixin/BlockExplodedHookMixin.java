package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.BLOCK_EXPLODED}): 1.7.10 coremods put a hook last in {@code Block.onBlockDestroyedByExplosion}
 * (drops of vanilla blocks an explosion destroys). A no-op without such a hook.
 */
@CoremodHook
@Mixin(Block.class)
public abstract class BlockExplodedHookMixin {
    @Inject(method = "wasExploded", at = @At("TAIL"), require = 0)
    private void legacyconnector$coremodExploded(Level level, BlockPos pos, Explosion explosion, CallbackInfo ci) {
        if (CoremodSites.BLOCK_EXPLODED.listed()) CoremodSites.blockExploded((Block) (Object) this, level, pos);
    }
}
