package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.Mob;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.AI_TASKS}): 1.7.10 coremods put a hook first in {@code EntityLiving.updateAITasks} that may run instead of
 * the mob's AI. A no-op without such a hook.
 */
@CoremodHook
@Mixin(Mob.class)
public abstract class MobAiHookMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$coremodAiTasks(CallbackInfo ci) {
        if (CoremodSites.AI_TASKS.listed() && CoremodSites.aiTasks((Mob) (Object) this)) ci.cancel();
    }
}
