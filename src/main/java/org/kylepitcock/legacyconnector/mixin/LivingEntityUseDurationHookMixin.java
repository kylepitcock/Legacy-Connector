package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.ITEM_USE_DURATION}): 1.12.2 coremods put a hook around the use duration that
 * {@code EntityLivingBase.getItemInUseMaxCount} reads from the stack in use (eating speed, drawing time). A no-op without such a hook.
 */
@CoremodHook
@Mixin(LivingEntity.class)
public abstract class LivingEntityUseDurationHookMixin {
    @WrapOperation(method = "getTicksUsingItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getUseDuration(Lnet/minecraft/world/entity/LivingEntity;)I"), require = 0)
    private int legacyconnector$coremodUseDuration(ItemStack stack, LivingEntity user, Operation<Integer> original) {
        int duration = original.call(stack, user);
        return CoremodSites1122.ITEM_USE_DURATION.listed() ? CoremodSites1122.itemUseDuration((LivingEntity) (Object) this, duration) : duration;
    }
}
