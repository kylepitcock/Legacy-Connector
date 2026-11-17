/*
 * Copyright (c) 2026 kentvillamor08
 * Licensed under the MIT License.
 * See LICENSE in the project root.
 */
package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;

/**
 * Adapters for the Forge 1.20.1 EntityType spawn/create overloads that carried
 * a CompoundTag. The 1.21.1 target overloads no longer accept that argument;
 * a scoped Pending frame makes it available to the exact create invocation.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class EntityTypeCompat {
    private EntityTypeCompat() {}

    public static Entity create(
            EntityType type,
            ServerLevel level,
            CompoundTag tag,
            Consumer consumer,
            BlockPos pos,
            MobSpawnType spawnType,
            boolean alignPosition,
            boolean invertY
    ) {
        try (SpawnContext.Scope ignored = SpawnContext.pushPending(type, tag)) {
            return type.create(level, consumer, pos, spawnType, alignPosition, invertY);
        }
    }

    public static Entity spawn(
            EntityType type,
            ServerLevel level,
            CompoundTag tag,
            Consumer consumer,
            BlockPos pos,
            MobSpawnType spawnType,
            boolean alignPosition,
            boolean invertY
    ) {
        try (SpawnContext.Scope ignored = SpawnContext.pushPending(type, tag)) {
            return type.spawn(level, consumer, pos, spawnType, alignPosition, invertY);
        }
    }
}
