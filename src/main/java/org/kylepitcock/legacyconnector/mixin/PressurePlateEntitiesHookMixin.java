package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.phys.AABB;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook sites (generic, {@code CoremodSites.PRESSURE_ENTITIES} and {@code PRESSURE_IGNORED}): 1.7.10 coremods changed which entities press a
 * pressure plate (the entity list of the plate's box, and the entities that never trigger plates). A no-op without such a hook.
 */
@Mixin(BasePressurePlateBlock.class)
public abstract class PressurePlateEntitiesHookMixin {
    @Inject(method = "getEntityCount", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacyconnector$coremodPressureEntities(Level level, AABB box, Class<? extends Entity> cls, CallbackInfoReturnable<Integer> cir) {
        if (!CoremodSites.PRESSURE_ENTITIES.listed() && !CoremodSites.PRESSURE_IGNORED.listed()) return;
        Integer count = CoremodSites.pressureCount(level, box, cls);
        if (count != null) cir.setReturnValue(count);
    }

    /** {@code CoremodSites.PLATE_TICK}: 1.7.10's {@code updateTick} of pressure plates, last thing. */
    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void legacyconnector$coremodPlateTick(net.minecraft.world.level.block.state.BlockState state, net.minecraft.server.level.ServerLevel level,
            net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (CoremodSites.PLATE_TICK.listed()) CoremodSites.plateTick(state, level, pos);
    }
}
