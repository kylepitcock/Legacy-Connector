package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Coremod hook sites (generic, {@code CoremodSites.NEIGHBORS_START}, {@code NEIGHBORS_STOP}, {@code NEIGHBOR_NOTIFIED}): 1.7.10 coremods put hooks around
 * {@code World.notifyBlocksOfNeighborChange} and first in {@code World.notifyBlockOfNeighborChange}. No-ops without such hooks.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelNeighborHookMixin {
    @Inject(method = "updateNeighborsAt", at = @At("HEAD"), require = 0)
    private void legacyconnector$coremodNeighborsStart(BlockPos pos, Block block, CallbackInfo ci) {
        if (CoremodSites.NEIGHBORS_START.listed()) CoremodSites.neighbors(CoremodSites.NEIGHBORS_START, (Level) (Object) this, pos);
    }

    @Inject(method = "updateNeighborsAt", at = @At("TAIL"), require = 0)
    private void legacyconnector$coremodNeighborsStop(BlockPos pos, Block block, CallbackInfo ci) {
        if (CoremodSites.NEIGHBORS_STOP.listed()) CoremodSites.neighbors(CoremodSites.NEIGHBORS_STOP, (Level) (Object) this, pos);
    }

    @Inject(method = "neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"),
            require = 0)
    private void legacyconnector$coremodNeighborNotified(BlockPos pos, Block block, BlockPos from, CallbackInfo ci) {
        if (CoremodSites.NEIGHBOR_NOTIFIED.listed()) CoremodSites.neighbors(CoremodSites.NEIGHBOR_NOTIFIED, (Level) (Object) this, pos);
    }
}
