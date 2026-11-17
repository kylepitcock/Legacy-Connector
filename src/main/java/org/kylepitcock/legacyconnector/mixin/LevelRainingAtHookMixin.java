package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.RAINING_AT}): 1.12.2 coremods put a hook last in {@code World.isRainingAt} that answers for a
 * position (rain shelters). A no-op without such a hook.
 */
@CoremodHook
@Mixin(Level.class)
public abstract class LevelRainingAtHookMixin {
    @Inject(method = "isRainingAt", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$coremodRainingAt(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!CoremodSites1122.RAINING_AT.listed()) return;
        boolean value = cir.getReturnValueZ();
        boolean changed = CoremodSites1122.raining((Level) (Object) this, pos, value);
        if (changed != value) cir.setReturnValue(changed);
    }
}
