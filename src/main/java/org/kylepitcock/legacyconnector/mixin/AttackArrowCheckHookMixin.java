package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Coremod hook site (generic, {@code CoremodSites.ARROW_TARGET}): 1.7.10 coremods changed which entities the server treats as arrows when a player attacks
 * (arrows are not attackable). A hook can only take an entity out of that class here (the game's check then goes on to cast it). A no-op without such a hook.
 */
@Mixin(targets = "net.minecraft.server.network.ServerGamePacketListenerImpl$1")
public abstract class AttackArrowCheckHookMixin {
    @Final
    @Shadow(aliases = "val$entity", remap = false)
    private Entity val$entity;

    @ModifyExpressionValue(method = "onAttack", at = @At(value = "CONSTANT", args = "classValue=net/minecraft/world/entity/projectile/AbstractArrow", ordinal = 0), require = 0)
    private boolean legacyconnector$coremodAttackArrow(boolean arrow) {
        return arrow && !(CoremodSites.ARROW_TARGET.listed() && !CoremodSites.isArrowTarget(val$entity));
    }
}
