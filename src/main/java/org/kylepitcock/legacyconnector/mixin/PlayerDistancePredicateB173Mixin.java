package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.Entity;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerAbilitiesB173;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge visibility multipliers and target-change events cannot implement an arbitrary-coordinate distance query.
 * Beta's coordinate and entity overloads independently compute distance; only the coordinate overload owns this hook.
 * Native entity distance delegates to Vec3 instead, so neither of those modern overloads is adapted here.
 */
@Mixin(Entity.class)
public abstract class PlayerDistancePredicateB173Mixin {
    @Inject(method="distanceToSqr(DDD)D",at=@At("RETURN"),cancellable=true)
    private void legacyconnector$betaDistance(double x,double y,double z,CallbackInfoReturnable<Double> result){
        result.setReturnValue(PlayerAbilitiesB173.nativeDistance((Entity)(Object)this,x,y,z,result.getReturnValue()));
    }
}
