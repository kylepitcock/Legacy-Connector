package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.ALL_ASLEEP}): 1.12.2 coremods put a hook first in {@code WorldServer.areAllPlayersAsleep} whose
 * answer (0: the game decides, 1: no, 2: yes) stands in for the check that skips the night. A no-op without such a hook.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelSleepHookMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/SleepStatus;areEnoughSleeping(I)Z"), require = 0)
    private boolean legacyconnector$coremodEnoughSleeping(SleepStatus status, int percentage, Operation<Boolean> original) {
        int answer = CoremodSites1122.ALL_ASLEEP.listed() ? CoremodSites1122.allAsleep((ServerLevel) (Object) this) : 0;
        return answer == 0 ? original.call(status, percentage) : answer == 2;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/SleepStatus;areEnoughDeepSleeping(ILjava/util/List;)Z"), require = 0)
    private boolean legacyconnector$coremodEnoughDeepSleeping(SleepStatus status, int percentage, List<ServerPlayer> players, Operation<Boolean> original) {
        int answer = CoremodSites1122.ALL_ASLEEP.listed() ? CoremodSites1122.allAsleep((ServerLevel) (Object) this) : 0;
        return answer != 0 || original.call(status, percentage, players);
    }
}
