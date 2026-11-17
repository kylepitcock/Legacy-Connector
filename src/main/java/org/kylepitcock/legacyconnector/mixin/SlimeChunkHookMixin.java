package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.LevelAccessor;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.kylepitcock.legacyconnector.mixinplugin.CoremodHook;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.SLIME_CHUNK}): 1.12.2 coremods put a hook early in {@code EntitySlime.getCanSpawnHere} that decides
 * whether the chunk is a slime chunk (0 no, 1 yes, -1 the game decides). A no-op without such a hook.
 */
@CoremodHook
@Mixin(Slime.class)
public abstract class SlimeChunkHookMixin {
    @Inject(method = "checkSlimeSpawnRules", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacyconnector$coremodSlimeChunk(EntityType<Slime> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random,
            CallbackInfoReturnable<Boolean> cir) {
        if (!CoremodSites1122.SLIME_CHUNK.listed()) return;
        int answer = CoremodSites1122.slimeChunk(level, pos);
        if (answer >= 0) cir.setReturnValue(answer != 0);
    }
}
