package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.level.LevelTimeAccess;
import org.kylepitcock.legacyconnector.vb1_7_3.DimensionTimeB173;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Changes celestial time only for levels owned by registered Beta dimensions. */
@Mixin(LevelTimeAccess.class)
public interface LevelTimeAccessB173Mixin {
    @Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
    private void legacyconnector$betaTime(float partialTick, CallbackInfoReturnable<Float> callback) {
        float current = callback.getReturnValue();
        float time = DimensionTimeB173.timeOfDay((LevelTimeAccess) this, partialTick, current);
        if (Float.floatToIntBits(time) != Float.floatToIntBits(current)) callback.setReturnValue(time);
    }
}
