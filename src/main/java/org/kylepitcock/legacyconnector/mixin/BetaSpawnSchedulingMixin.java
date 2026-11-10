package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.kylepitcock.legacyconnector.vb1_7_3.SpawnSchedulerB173;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps Beta scheduling inside the native tick's eligibility and gamerule gates. */
@Mixin(ServerChunkCache.class)
public abstract class BetaSpawnSchedulingMixin {
    @Shadow @Final private ServerLevel level;

    @WrapMethod(method="tickChunks")
    private void legacyconnector$spawnCycle(Operation<Void> original) {
        try(var scope=SpawnSchedulerB173.cycle(level)) {
            original.call();
        }
    }

    @WrapOperation(method="tickChunks",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/NaturalSpawner;spawnForChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZZ)V"))
    private void legacyconnector$ordinarySpawns(ServerLevel world,LevelChunk chunk,NaturalSpawner.SpawnState state,
            boolean friendlies,boolean enemies,boolean persistent,Operation<Void> original) {
        SpawnSchedulerB173.ordinary(world,chunk,state,friendlies,enemies,
                ()->original.call(world,chunk,state,friendlies,enemies,persistent));
    }


}
