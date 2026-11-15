package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.server.level.ServerLevel;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.PLAYERS_WAKE}): 1.12.2 coremods put a hook first in {@code WorldServer.wakeAllPlayers} (the night was
 * slept through). A no-op without such a hook.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelWakeHookMixin {
    @Inject(method = "wakeUpAllPlayers", at = @At("HEAD"), require = 0)
    private void legacyconnector$coremodPlayersWake(CallbackInfo ci) {
        if (CoremodSites1122.PLAYERS_WAKE.listed()) CoremodSites1122.playersWake((ServerLevel) (Object) this);
    }
}
