package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7.10's bow posted ArrowLooseEvent on every release, before it looked for arrows; NeoForge posts its ArrowLooseEvent only when the
 * bow has vanilla ammo. Legacy handlers that shoot their own ammo from the vanilla bow (Thaumcraft's elemental arrows, #225) get the
 * legacy event here. A no-op without legacy ArrowLooseEvent handlers.
 */
@Mixin(BowItem.class)
public abstract class BowItemLegacyLooseMixin {
    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void legacyconnector$legacyArrowLoose(ItemStack stack, Level level, LivingEntity entity, int timeLeft, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        int charge = ((BowItem) (Object) this).getUseDuration(stack, entity) - timeLeft;
        if (org.kylepitcock.legacyconnector.v1_7_10.bridge.EventBridge1710.mcc$arrowLoose(stack, player, charge)) ci.cancel();
    }
}
