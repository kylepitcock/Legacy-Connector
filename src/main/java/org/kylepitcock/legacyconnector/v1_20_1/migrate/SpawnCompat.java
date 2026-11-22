/*
 * Copyright (c) 2026 kentvillamor08
 * Licensed under the MIT License.
 * See LICENSE in the project root.
 */
package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;

/** Compatibility entry points for direct Forge 1.20.1 Mob spawn-finalization calls. */
public final class SpawnCompat {
    private SpawnCompat() {}

    /**
     * Preserves the removed fifth argument around a virtual target call. The
     * generated 1.21.1-shaped bridge on a translated Mob subclass reads the
     * tag from the Active frame and redispatches to its legacy override.
     */
    public static SpawnGroupData finalizeSpawn(
            Mob mob,
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            SpawnGroupData groupData,
            CompoundTag tag
    ) {
        try (SpawnContext.Scope ignored = SpawnContext.pushActive(mob, tag)) {
            return mob.finalizeSpawn(level, difficulty, spawnType, groupData);
        }
    }
}
