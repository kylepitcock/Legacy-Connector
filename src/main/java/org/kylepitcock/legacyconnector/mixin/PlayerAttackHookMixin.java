package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.PLAYER_ATTACK}): 1.7.10 coremods replaced the {@code target.attackEntityFrom(source, amount)} of
 * {@code EntityPlayer.attackTargetEntityWithCurrentItem} with a hook of their own (which changes the damage and then damages the target itself).
 * A no-op without such a hook.
 */
@CoremodHook
@Mixin(Player.class)
public abstract class PlayerAttackHookMixin {
    @WrapOperation(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), require = 0)
    private boolean legacyconnector$coremodAttack(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
        if (CoremodSites.PLAYER_ATTACK.listed()) {
            Boolean result = CoremodSites.playerAttack((Player) (Object) this, target, source, amount);
            if (result != null) return result;
        }
        return original.call(target, source, amount);
    }
}
