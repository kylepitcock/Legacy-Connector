package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks the dimension refresh and tick seams without changing any player sizing values. */
@Mixin(Entity.class)
public abstract class AlienEvoDimensionsProbeMixin {
    @Inject(method = "refreshDimensions", at = @At("HEAD"), require = 0)
    private void legacyconnector$beforeRefreshDimensions(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player player) {
            AlienEvoDimensionsProbe.record(player, "before");
        }
    }

    @Inject(method = "refreshDimensions", at = @At("TAIL"), require = 0)
    private void legacyconnector$afterRefreshDimensions(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player player) {
            AlienEvoDimensionsProbe.record(player, "after");
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void legacyconnector$sampleChangedPlayerSize(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player player) {
            AlienEvoDimensionsProbe.sample(player);
        }
    }
}
