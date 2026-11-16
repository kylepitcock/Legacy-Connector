package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.Entity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.TRACKING_RANGE}): 1.7.10 coremods changed the range an entity is tracked at in
 * {@code EntityTracker.addEntityToTracker} (entities that must stay visible far away). The modern range still passes the server's view distance
 * ({@code scaledRange}), as 1.7.10's maximum did. A no-op without such a hook.
 */
@CoremodHook
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityRangeHookMixin {
    @Final
    @Shadow
    Entity entity;

    @ModifyArg(method = "getEffectiveRange", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap$TrackedEntity;scaledRange(I)I"), require = 0)
    private int legacyconnector$coremodTrackingRange(int range) {
        return CoremodSites.TRACKING_RANGE.listed() ? CoremodSites.trackingRange(entity, range) : range;
    }

    /** {@code CoremodSites.TRACKING_MAX}: the cap of the tracking range (the player's view distance, in blocks). */
    @ModifyArg(method = "updatePlayer", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"), index = 1, require = 0)
    private int legacyconnector$coremodTrackingMax(int max) {
        return CoremodSites.TRACKING_MAX.listed() ? CoremodSites.trackingMax(entity, max) : max;
    }
}
