package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook sites (generic, {@code CoremodSites.Client.NIGHT_VISION_ACTIVE}, {@code NIGHT_VISION_AMOUNT}): 1.7.10 coremods changed whether the light map
 * counts the player as having night vision, and how strong the effect is (night vision without the potion). A no-op without such hooks.
 */
@CoremodHook
@Mixin(LightTexture.class)
public abstract class LightTextureNightVisionHookMixin {
    @WrapOperation(method = "updateLightTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z"), require = 0)
    private boolean legacyconnector$coremodNightVision(LocalPlayer player, Holder<MobEffect> effect, Operation<Boolean> original) {
        boolean has = original.call(player, effect);
        if (effect.equals(MobEffects.NIGHT_VISION) && CoremodSites.Client.NIGHT_VISION_ACTIVE.listed()) return CoremodSites.Client.nightVisionActive(has);
        return has;
    }

    @WrapOperation(method = "updateLightTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"), require = 0)
    private float legacyconnector$coremodNightVisionAmount(LivingEntity entity, float partialTick, Operation<Float> original) {
        // vanilla's scale needs the effect; without it (a hook turned night vision on) the base amount is nothing
        float amount = entity.hasEffect(MobEffects.NIGHT_VISION) ? original.call(entity, partialTick) : 0.0F;
        return CoremodSites.Client.NIGHT_VISION_AMOUNT.listed() ? CoremodSites.Client.nightVisionAmount(entity, amount, partialTick) : amount;
    }
}
