package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.SLOW_FALL}): the client slows an entity down to a gentle fall while the chunk under it is not loaded yet
 * ({@code isClientSide && !hasChunkAt}); 1.7.10 coremods put a hook behind the "is this the client" check that took an entity out of it. The hook runs
 * only where the game would slow the entity down (a client, an unloaded chunk). A no-op without such a hook.
 */
@CoremodHook
@Mixin(LivingEntity.class)
public abstract class UnloadedChunkSlowFallHookMixin {
    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasChunkAt(Lnet/minecraft/core/BlockPos;)Z"), require = 0)
    private boolean legacyconnector$coremodSlowFall(boolean loaded) {
        return loaded || (CoremodSites.SLOW_FALL.listed() && CoremodSites.noSlowFall());
    }
}
