package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.Mob;
import org.kylepitcock.legacyconnector.vb1_7_3.B173LivingHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Beta's virtual AI superclass call owns navigation/control; skipping it must skip that work too. */
@Mixin(Mob.class)
abstract class BetaAnimalAiStepMixin {
    @Inject(method="serverAiStep",at=@At("HEAD"),cancellable=true)
    private void mcc$legacyAnimalAI(CallbackInfo ci){
        if((Object)this instanceof B173LivingHost host){host.serverAIPhase();ci.cancel();}
    }
}
