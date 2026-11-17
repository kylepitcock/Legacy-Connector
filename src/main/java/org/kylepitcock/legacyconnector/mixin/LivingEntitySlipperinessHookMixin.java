package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.SLIPPERINESS}): 1.12.2 coremods put a hook on the ground friction of {@code EntityLivingBase.travel}
 * (slipperiness times 0.91), receiving the entity and the block below. A no-op without such a hook.
 */
@CoremodHook
@Mixin(LivingEntity.class)
public abstract class LivingEntitySlipperinessHookMixin {
    @WrapOperation(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getFriction(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)F"),
            require = 0)
    private float legacyconnector$coremodSlipperiness(BlockState ground, LevelReader level, BlockPos pos, Entity entity, Operation<Float> original) {
        float friction = original.call(ground, level, pos, entity);
        return CoremodSites1122.SLIPPERINESS.listed() ? CoremodSites1122.slipperiness((LivingEntity) (Object) this, ground, friction) : friction;
    }
}
