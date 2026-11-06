package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.v1_7_10.compat.am2.Am2Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class Am2CameraPositionMixin {
    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;
    @Shadow public abstract Vec3 getPosition();
    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", ordinal = 0, shift = At.Shift.AFTER))
    private void legacyconnector$alignAm2Eyes(BlockGetter level, Entity entity, boolean detached, boolean reverse,
            float partialTick, CallbackInfo ci) {
        if (entity != Minecraft.getInstance().player) return;
        float roll = Am2Camera.rollFor(entity, partialTick);
        if (roll == 0.0F) return;
        var position = getPosition();
        double offset = Am2Camera.eyeOffset(roll, entity.getBbHeight(), Mth.lerp(partialTick, eyeHeightOld, eyeHeight));
        setPosition(position.x, position.y + offset, position.z);
    }
}
