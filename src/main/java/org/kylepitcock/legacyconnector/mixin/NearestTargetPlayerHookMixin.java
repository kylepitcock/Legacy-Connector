package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.CLOSEST_VULNERABLE}): 1.7.10 coremods replaced the world's "closest player a mob may attack" lookup.
 * Where a mob's nearest-target goal looks for players, the hook's answer is the target. A no-op without such a hook.
 */
@CoremodHook
@Mixin(NearestAttackableTargetGoal.class)
public abstract class NearestTargetPlayerHookMixin extends TargetGoal {
    @Shadow
    protected Class<?> targetType;
    @Shadow
    protected LivingEntity target;

    protected NearestTargetPlayerHookMixin(Mob mob, boolean mustSee) {
        super(mob, mustSee);
    }

    @Inject(method = "findTarget", at = @At("TAIL"), require = 0)
    private void legacyconnector$coremodClosestVulnerable(CallbackInfo ci) {
        if (targetType != Player.class || !CoremodSites.CLOSEST_VULNERABLE.listed()) return;
        Object[] answer = CoremodSites.closestVulnerable(this.mob, this.getFollowDistance());
        if (answer == null) return;
        this.target = answer[0] instanceof legacyconnector.v1_7_10.net.minecraft.entity.Entity legacy && legacy.mcc$entity instanceof Player player ? player : null;
    }
}
