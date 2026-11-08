package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NaturalSpawner.SpawnState.class)
public interface BetaSpawnStateAccess {
    @Invoker("canSpawn")
    boolean legacyconnector$canSpawn(EntityType<?> type,BlockPos pos,ChunkAccess chunk);

    @Invoker("afterSpawn")
    void legacyconnector$afterSpawn(Mob mob,ChunkAccess chunk);
}
