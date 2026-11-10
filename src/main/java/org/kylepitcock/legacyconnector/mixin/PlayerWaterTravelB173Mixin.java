package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerAbilitiesB173;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's extra fluid-membership branches otherwise bypass the original water predicate.
 * FluidType.move belongs to the shared fluid type, with no per-player event to change branch membership;
 * changing the global water type would affect unrelated entities and fluid behavior.
 */
@Mixin(LivingEntity.class)
public abstract class PlayerWaterTravelB173Mixin {
    @ModifyExpressionValue(method="travel",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;isInFluidType(Lnet/minecraft/world/level/material/FluidState;)Z"),require=2,allow=2)
    private boolean legacyconnector$betaWaterMembership(boolean baseline,@Local FluidState fluid){
        return PlayerAbilitiesB173.travelWaterMembership((LivingEntity)(Object)this,fluid.is(FluidTags.WATER),baseline);
    }
}
