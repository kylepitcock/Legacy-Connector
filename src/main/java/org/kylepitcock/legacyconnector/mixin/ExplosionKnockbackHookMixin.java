package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.Explosion;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.EXPLOSION_KNOCKBACK}): 1.7.10 coremods replace the call that reduces a living entity's blast knockback by
 * its armour's protection. The modern game applies the reduction as the entity's explosion knockback resistance attribute; the hook gets the factor before
 * it (local 24) and the resistance is then derived so the modern game ends up with the hook's answer. A no-op without such a hook.
 */
@CoremodHook
@Mixin(Explosion.class)
public abstract class ExplosionKnockbackHookMixin {
    @WrapOperation(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"), require = 0)
    private double legacyconnector$coremodKnockback(LivingEntity entity, Holder<Attribute> attribute, Operation<Double> original, @Local(index = 24) double factor) {
        double resistance = original.call(entity, attribute);
        if (!CoremodSites.EXPLOSION_KNOCKBACK.listed() || factor == 0.0) return resistance;
        double changed = CoremodSites.explosionKnockback(entity, factor);
        return changed == factor ? resistance : 1.0 - changed / factor; // the game multiplies the factor by (1 - resistance)
    }
}
