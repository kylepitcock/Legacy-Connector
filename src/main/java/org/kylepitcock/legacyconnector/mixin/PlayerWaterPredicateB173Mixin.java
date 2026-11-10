package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** NeoForge fluid-motion extensions do not expose the vanilla water predicate queried by PlayerAPI. */
@Mixin(Entity.class)
public abstract class PlayerWaterPredicateB173Mixin {
    @Inject(method="isInWater",at=@At("RETURN"),cancellable=true)
    private void legacyconnector$b173Water(CallbackInfoReturnable<Boolean> result){
        if((Object)this instanceof Player player)result.setReturnValue(org.kylepitcock.legacyconnector.vb1_7_3.PlayerAbilitiesB173.nativeWater(player,result.getReturnValue()));
    }
}
