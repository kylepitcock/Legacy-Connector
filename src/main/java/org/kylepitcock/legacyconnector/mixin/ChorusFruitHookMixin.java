package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ChorusFruitItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.CHORUS_EATEN}): 1.12.2 coremods put a hook where the chorus fruit starts its random teleport; a hook
 * answering true took the teleport over. A no-op without such a hook.
 */
@Mixin(ChorusFruitItem.class)
public abstract class ChorusFruitHookMixin {
    @Inject(method = "finishUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0), cancellable = true, require = 0)
    private void legacyconnector$coremodChorus(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir,
            @Local(ordinal = 1) ItemStack result) {
        if (CoremodSites1122.CHORUS_EATEN.listed() && CoremodSites1122.chorusEaten(level, entity)) cir.setReturnValue(result);
    }
}
