package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook sites (generic, {@code CoremodSites1122.EXHAUSTION_*}): 1.12.2 coremods that rework hunger put a hook at each constant of the exhaustion a
 * player's action causes (jumping, attacking, taking damage); it answers the amount. The place also tells the hook which action it is. No-ops without hooks.
 */
@CoremodHook
@Mixin(Player.class)
public abstract class PlayerExhaustionHookMixin {
    private static final String CAUSE = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V";

    /** The sprinting jump (0.2) is the first of the two calls, the plain jump (0.05) the second. */
    @ModifyArg(method = "jumpFromGround", at = @At(value = "INVOKE", target = CAUSE, ordinal = 0), index = 0, require = 0)
    private float legacyconnector$coremodSprintJump(float amount) {
        return CoremodSites1122.EXHAUSTION_SPRINT_JUMP.listed() ? CoremodSites1122.exhaustion(CoremodSites1122.EXHAUSTION_SPRINT_JUMP, (Player) (Object) this, amount) : amount;
    }

    @ModifyArg(method = "jumpFromGround", at = @At(value = "INVOKE", target = CAUSE, ordinal = 1), index = 0, require = 0)
    private float legacyconnector$coremodJump(float amount) {
        return CoremodSites1122.EXHAUSTION_JUMP.listed() ? CoremodSites1122.exhaustion(CoremodSites1122.EXHAUSTION_JUMP, (Player) (Object) this, amount) : amount;
    }

    @ModifyArg(method = "attack", at = @At(value = "INVOKE", target = CAUSE, ordinal = 0), index = 0, require = 0)
    private float legacyconnector$coremodAttack(float amount) {
        return CoremodSites1122.EXHAUSTION_ATTACK.listed() ? CoremodSites1122.exhaustion(CoremodSites1122.EXHAUSTION_ATTACK, (Player) (Object) this, amount) : amount;
    }

    @ModifyArg(method = "actuallyHurt", at = @At(value = "INVOKE", target = CAUSE, ordinal = 0), index = 0, require = 0)
    private float legacyconnector$coremodDamage(float amount) {
        return CoremodSites1122.EXHAUSTION_DAMAGE.listed() ? CoremodSites1122.exhaustion(CoremodSites1122.EXHAUSTION_DAMAGE, (Player) (Object) this, amount) : amount;
    }
}
