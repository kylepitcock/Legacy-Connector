package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.BRIGHTNESS_Y}): 1.7.10 coremods replaced the block height {@code Entity.getBrightnessForRender} reads the
 * light of a rendered entity at (an intangible entity inside blocks, a scaled one). The modern renderer reads it at {@code getLightProbePosition}. A no-op
 * without such a hook.
 */
@CoremodHook
@Mixin(Entity.class)
public abstract class EntityLightProbeHookMixin {
    @Inject(method = "getLightProbePosition", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodLightHeight(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        if (!CoremodSites.BRIGHTNESS_Y.listed()) return;
        Vec3 probe = cir.getReturnValue();
        Vec3 changed = CoremodSites.lightProbe((Entity) (Object) this, probe);
        if (changed != probe) cir.setReturnValue(changed);
    }
}
