package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sizes legacy mods gave a player or vanilla mob with {@code Entity.setSize} (see {@code EntitySizes1710}). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDimensionsMixin {
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$legacySize(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        EntityDimensions original = cir.getReturnValue();
        EntityDimensions sized = org.kylepitcock.legacyconnector.v1_7_10.entity.EntitySizes1710.apply((LivingEntity) (Object) this, original);
        if (sized != original) cir.setReturnValue(sized);
    }
}
