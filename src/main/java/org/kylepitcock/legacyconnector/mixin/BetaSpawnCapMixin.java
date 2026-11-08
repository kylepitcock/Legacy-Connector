package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.MobCategory;
import org.kylepitcock.legacyconnector.vb1_7_3.SpawnCapsB173;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobCategory.class)
public abstract class BetaSpawnCapMixin {
    @Inject(method="getMaxInstancesPerChunk",at=@At("RETURN"),cancellable=true)
    private void legacyconnector$betaSpawnCap(CallbackInfoReturnable<Integer> result) {
        result.setReturnValue(SpawnCapsB173.capacity((MobCategory)(Object)this,result.getReturnValue()));
    }
}
