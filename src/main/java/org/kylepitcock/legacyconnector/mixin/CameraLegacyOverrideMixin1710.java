package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacyRendererOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The camera takes the eye height ({@code yOffset}) and the third person distance a legacy mod's {@code EntityRenderer} subclass left in the legacy objects
 * (see {@link LegacyRendererOverrides}). A no-op without such a subclass.
 */
@Mixin(Camera.class)
public abstract class CameraLegacyOverrideMixin1710 {
    @Shadow
    public abstract Vec3 getPosition();

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"), require = 0)
    private float legacyconnector$thirdPersonDistance(float normal) {
        return LegacyRendererOverrides.thirdPersonDistance(normal);
    }

    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void legacyconnector$eyeHeight(BlockGetter level, Entity entity, boolean detached, boolean reverse, float partialTick, CallbackInfo ci) {
        double raise = LegacyRendererOverrides.cameraRaise(entity);
        if (raise == 0.0) return;
        Vec3 p = getPosition();
        setPosition(p.x, p.y + raise, p.z);
    }
}
