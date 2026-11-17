package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Abilities;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.Client.FOV_WALK_SPEED}): 1.7.10 coremods replaced the {@code capabilities.getWalkSpeed()} of
 * {@code EntityPlayerSP.getFOVMultiplier} (the field of view follows the player's speed). A no-op without such a hook.
 */
@CoremodHook
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerFovHookMixin {
    @WrapOperation(method = "getFieldOfViewModifier", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Abilities;getWalkingSpeed()F"), require = 0)
    private float legacyconnector$coremodFovWalkSpeed(Abilities abilities, Operation<Float> original) {
        float speed = original.call(abilities);
        return CoremodSites.Client.FOV_WALK_SPEED.listed() ? CoremodSites.Client.fovWalkSpeed((AbstractClientPlayer) (Object) this, speed) : speed;
    }
}
