package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Entity, block and registry-object calls of 1.20.1 that 1.21.1 reshaped (holders instead of objects, vectors instead of three doubles, an extra size argument). */
public final class GameObjectCompat {
    private GameObjectCompat() {}

    /** {@code Cat.setVariant(CatVariant)}: variants are registry holders now. */
    public static void setCatVariant(Cat cat, CatVariant variant) {
        cat.setVariant(BuiltInRegistries.CAT_VARIANT.wrapAsHolder(variant));
    }

    /** {@code new WitherSkull(level, owner, dx, dy, dz)}: the acceleration is one vector. */
    public static WitherSkull newWitherSkull(Level level, LivingEntity owner, double dx, double dy, double dz) {
        return new WitherSkull(level, owner, new Vec3(dx, dy, dz));
    }

    /** {@code FlowerBlock.getSuspiciousEffect()}: the effect of the first suspicious stew entry (1.21.1 lets a flower give several). */
    public static MobEffect flowerEffect(FlowerBlock flower) {
        SuspiciousStewEffects.Entry first = flower.getSuspiciousEffects().effects().get(0);
        return first.effect().value();
    }

    /** {@code FlowerBlock.getEffectDuration()}: duration in ticks of the first stew entry. */
    public static int flowerEffectDuration(FlowerBlock flower) {
        return flower.getSuspiciousEffects().effects().get(0).duration();
    }

    /** {@code Container.stillValidBlockEntity(blockEntity, player, distance)}: the block entity is still in place and the player within {@code distance} blocks of its centre. */
    public static boolean stillValidBlockEntity(BlockEntity entity, Player player, int distance) {
        Level level = entity.getLevel();
        if (level == null || level.getBlockEntity(entity.getBlockPos()) != entity) return false;
        BlockPos pos = entity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= (double) (distance * distance);
    }

    /** {@code Shulker.getProgressAabb(direction, progress)}: 1.21.1 scales the box by the entity's size; 1.20.1's boxes were for scale 1. */
    public static AABB shulkerProgressAabb(Direction direction, float progress) {
        return Shulker.getProgressAabb(1.0F, direction, progress);
    }

    public static AABB shulkerProgressDeltaAabb(Direction direction, float from, float to) {
        return Shulker.getProgressDeltaAabb(1.0F, direction, from, to);
    }

    /** {@code Block.isBed(state, level, pos, entity)}: NeoForge's state extension takes a living entity (null for other entities). */
    public static boolean isBed(Block block, BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return state.isBed(level, pos, entity instanceof LivingEntity living ? living : null);
    }

    /** {@code GameEvent.builtInRegistryHolder()}: the holder of the event in its registry. */
    public static Holder.Reference<GameEvent> gameEventHolder(GameEvent event) {
        return BuiltInRegistries.GAME_EVENT.getHolderOrThrow(BuiltInRegistries.GAME_EVENT.getResourceKey(event).orElseThrow());
    }
}
