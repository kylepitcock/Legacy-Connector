package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MOD-SPECIFIC COMPAT (Fisk's Superheroes): its coremod called {@code ASMHooks.playStepSound} on every step so a suit can make its own
 * footstep sounds. See {@code compat.fiskheroes.FiskHeroesHooks}; a no-op without the mod.
 */
@Mixin(Player.class)
public abstract class PlayerStepSoundMixin {
    @Inject(method = "playStepSound", at = @At("HEAD"), require = 0)
    private void legacyconnector$fiskHeroesStep(BlockPos pos, BlockState state, CallbackInfo ci) {
        org.kylepitcock.legacyconnector.v1_7_10.compat.fiskheroes.FiskHeroesHooks.stepSound((Player) (Object) this, pos);
    }
}
