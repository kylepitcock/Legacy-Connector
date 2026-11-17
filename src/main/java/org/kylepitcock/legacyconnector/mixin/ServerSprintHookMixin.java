package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites.SPRINT_STARTED}): 1.7.10 coremods put a hook after the server started a player's sprint in
 * {@code NetHandlerPlayServer.processEntityAction}. A no-op without such a hook.
 */
@CoremodHook
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerSprintHookMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setSprinting(Z)V", ordinal = 0, shift = At.Shift.AFTER),
            require = 0)
    private void legacyconnector$coremodSprint(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        if (CoremodSites.SPRINT_STARTED.listed()) CoremodSites.sprintStarted(player);
    }
}
