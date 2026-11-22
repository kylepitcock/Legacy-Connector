package com.example.legacyspawnprobe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * A real Forge 1.20.1 Mob subclass with the removed five-argument override.
 * Its super call deliberately compiles as INVOKESPECIAL so the probe also
 * exercises the transformer's non-virtual superclass-call adaptation.
 */
public final class SpawnProbeMob extends Zombie {
    public SpawnProbeMob(EntityType<? extends SpawnProbeMob> type, Level level) {
        super(type, level);
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType reason,
            SpawnGroupData groupData,
            CompoundTag tag
    ) {
        SpawnProbe.recordFinalize(tag);
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData, tag);
        SpawnProbe.recordSuperReturn(tag);

        if (tag != null && tag.getBoolean("spawn_nested")) {
            SpawnProbe.runNested(level, blockPosition());
        }
        return result;
    }
}
