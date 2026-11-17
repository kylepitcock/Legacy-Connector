package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.client.Minecraft;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.Client.PICK_BLOCK}): 1.7.10 coremods put a hook right after {@code Minecraft.runTick} handled the pick block
 * key. A no-op without such a hook.
 */
@CoremodHook
@Mixin(Minecraft.class)
public abstract class ClientPickBlockHookMixin {
    @Inject(method = "pickBlock", at = @At("TAIL"), require = 0)
    private void legacyconnector$coremodPickBlock(CallbackInfo ci) {
        if (CoremodSites.Client.PICK_BLOCK.active()) CoremodSites.Client.PICK_BLOCK.fire();
    }
}
