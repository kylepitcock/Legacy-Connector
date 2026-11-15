package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.STACK_COMPONENT}): 1.12.2 coremods put a hook last in {@code ItemStack.getTextComponent} (the
 * bracketed, hoverable item name of chat) that answers the component. A no-op without such a hook.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackComponentHookMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodStackComponent(CallbackInfoReturnable<Component> cir) {
        if (!CoremodSites1122.STACK_COMPONENT.listed()) return;
        Component original = cir.getReturnValue();
        Component changed = CoremodSites1122.stackComponent(original);
        if (changed != original) cir.setReturnValue(changed);
    }
}
