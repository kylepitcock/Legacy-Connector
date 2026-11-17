package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.level.Level;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.RAIN_STRENGTH}): 1.7.10 coremods put a hook into {@code World.getRainStrength} that answers for the
 * world (dimensions without rain). A no-op without such a hook.
 */
@CoremodHook
@Mixin(Level.class)
public abstract class LevelRainHookMixin {
    @Inject(method = "getRainLevel", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodRain(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (!CoremodSites.RAIN_STRENGTH.listed()) return;
        float strength = cir.getReturnValueF();
        float changed = CoremodSites.rainStrength((Level) (Object) this, strength, partialTick);
        if (changed != strength) cir.setReturnValue(changed);
    }
}
