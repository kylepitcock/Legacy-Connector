package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook sites (generic, {@code CoremodSites.ITEM_TICK}, {@code ITEM_USED}): 1.7.10 coremods put hooks last in {@code ItemStack.updateAnimation}
 * (every stack of an inventory, each tick) and after a use on a block that counted in {@code ItemStack.tryPlaceItemIntoWorld}. No-ops without such hooks.
 */
@CoremodHook
@Mixin(ItemStack.class)
public abstract class ItemStackHookMixin {
    @Inject(method = "inventoryTick", at = @At("TAIL"), require = 0)
    private void legacyconnector$coremodItemTick(Level level, Entity entity, int slot, boolean held, CallbackInfo ci) {
        if (CoremodSites.ITEM_TICK.listed()) CoremodSites.itemTick((ItemStack) (Object) this, entity, slot, held);
    }

    @Inject(method = "useOn", at = @At("RETURN"), require = 0)
    private void legacyconnector$coremodItemUsed(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (CoremodSites.ITEM_USED.listed() && cir.getReturnValue().indicateItemUse()) CoremodSites.itemUsed((ItemStack) (Object) this, context);
    }
}
