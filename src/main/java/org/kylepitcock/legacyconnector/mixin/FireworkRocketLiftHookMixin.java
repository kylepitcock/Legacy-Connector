package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Coremod hook site (generic, {@code CoremodSites.FIREWORK_LIFT}): 1.7.10 coremods replaced the 0.04 blocks per tick a firework rocket rises with.
 * A no-op without such a hook.
 */
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketLiftHookMixin {
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.04), require = 0)
    private double legacyconnector$coremodFireworkLift(double lift) {
        return CoremodSites.FIREWORK_LIFT.listed() ? CoremodSites.fireworkLift((FireworkRocketEntity) (Object) this, lift) : lift;
    }
}
