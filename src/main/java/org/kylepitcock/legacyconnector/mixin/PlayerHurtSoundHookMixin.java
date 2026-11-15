package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook site (generic, {@code CoremodSites.PLAYER_HURT_SOUND}): 1.7.10 coremods replaced the "game.player.hurt" name {@code EntityPlayer.getHurtSound}
 * returned. A no-op without such a hook.
 */
@Mixin(Player.class)
public abstract class PlayerHurtSoundHookMixin {
    @Inject(method = "getHurtSound", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        if (CoremodSites.PLAYER_HURT_SOUND.listed()) cir.setReturnValue(CoremodSites.playerHurtSound((Player) (Object) this, cir.getReturnValue()));
    }
}
