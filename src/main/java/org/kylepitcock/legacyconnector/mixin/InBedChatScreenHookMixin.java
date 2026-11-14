package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.gui.screens.InBedChatScreen;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Coremod hook site (generic, {@code CoremodSites.Client.SLEEP_CANCELLED}): 1.7.10 coremods put a hook last in {@code GuiSleepMP.wakeFromSleep}, the
 * "Leave Bed" button (sleeping places of their own that the server does not know as a bed). A no-op without such a hook.
 */
@Mixin(InBedChatScreen.class)
public abstract class InBedChatScreenHookMixin {
    @Inject(method = "sendWakeUp", at = @At("TAIL"), require = 0)
    private void legacyconnector$coremodLeaveBed(CallbackInfo ci) {
        if (CoremodSites.Client.SLEEP_CANCELLED.listed() && CoremodSites.Client.SLEEP_CANCELLED.active()) CoremodSites.Client.SLEEP_CANCELLED.fire();
    }
}
