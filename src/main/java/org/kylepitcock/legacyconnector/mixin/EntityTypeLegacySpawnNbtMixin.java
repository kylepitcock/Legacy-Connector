/*
 * Copyright (c) 2026 kentvillamor08
 * Licensed under the MIT License.
 * See LICENSE in the project root.
 */
package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.ItemStackNbt;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.SpawnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Carries Forge 1.20.1's EntityType spawn CompoundTag through the 1.21.1
 * create path without changing NeoForge's cancellation or stack-config order.
 */
@Mixin(EntityType.class)
public abstract class EntityTypeLegacySpawnNbtMixin {
    @WrapMethod(
            method = "create(Lnet/minecraft/server/level/ServerLevel;Ljava/util/function/Consumer;"
                    + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)"
                    + "Lnet/minecraft/world/entity/Entity;",
            require = 1
    )
    private Entity legacyconnector$trackCreateScope(
            ServerLevel level,
            Consumer<Entity> consumer,
            BlockPos pos,
            MobSpawnType spawnType,
            boolean alignPosition,
            boolean invertY,
            Operation<Entity> original
    ) {
        try (SpawnContext.Scope ignored = SpawnContext.enterCreate((Object) this)) {
            return original.call(level, consumer, pos, spawnType, alignPosition, invertY);
        }
    }

    /**
     * The consumer-based create method calls create(Level) to construct its
     * entity before it positions and finalizes that entity. Bind the active
     * Create frame to that exact returned instance here. This avoids wrapping
     * an invocation inside a method already wrapped by @WrapMethod.
     */
    @WrapMethod(
            method = "create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;",
            require = 1
    )
    private Entity legacyconnector$bindCreatedEntity(
            Level level,
            Operation<Entity> original
    ) {
        try (SpawnContext.Scope ignored = SpawnContext.enterFactory((Object) this)) {
            Entity entity = original.call(level);
            if (entity != null) {
                SpawnContext.bindCreatedEntity((Object) this, entity);
            }
            return entity;
        }
    }

    @WrapOperation(
            method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;"
                    + "Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityType;spawn("
                            + "Lnet/minecraft/server/level/ServerLevel;Ljava/util/function/Consumer;"
                            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)"
                            + "Lnet/minecraft/world/entity/Entity;"
            ),
            require = 1
    )
    private Entity legacyconnector$carryItemStackRootTag(
            EntityType<?> entityType,
            ServerLevel level,
            Consumer<Entity> consumer,
            BlockPos pos,
            MobSpawnType spawnType,
            boolean alignPosition,
            boolean invertY,
            Operation<Entity> original,
            ServerLevel enclosingLevel,
            ItemStack stack,
            Player player,
            BlockPos enclosingPos,
            MobSpawnType enclosingSpawnType,
            boolean enclosingAlignPosition,
            boolean enclosingInvertY
    ) {
        CompoundTag legacyTag = ItemStackNbt.getSpawnTag(stack, level.registryAccess());
        try (SpawnContext.Scope ignored = SpawnContext.pushPending(entityType, legacyTag)) {
            return original.call(entityType, level, consumer, pos, spawnType, alignPosition, invertY);
        }
    }
}
