package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.kylepitcock.legacyconnector.compat.alienevo.AlienEvoDimensions1201;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AlienEvo dev.24 targets Entity.getDimensions and Player.getDimensions.
 * On Minecraft 1.21.1, LivingEntity owns the final getDimensions override
 * inherited by Player, so those older hooks do not affect transformed players.
 */
@Mixin(LivingEntity.class)
public abstract class AlienEvoLivingDimensions1201Mixin {
    @Inject(
            method = "getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1)
    private void legacyconnector$applyAlienEvoDimensions(
            Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        EntityDimensions original = cir.getReturnValue();
        EntityDimensions scaled = AlienEvoDimensions1201.apply(entity, original);
        if (scaled != original) {
            cir.setReturnValue(scaled);
        }
    }
}
