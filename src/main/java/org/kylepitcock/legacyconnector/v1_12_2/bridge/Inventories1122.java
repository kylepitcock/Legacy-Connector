package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Small conversions the 1.12.2 inventory, block and item shims share: positions, levels, dropping stacks into the world. */
public final class Inventories1122 {
    private Inventories1122() {}

    public static net.minecraft.core.BlockPos pos(BlockPos p) {
        return new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ());
    }

    public static BlockPos legacyPos(net.minecraft.core.BlockPos p) {
        return new BlockPos(p.getX(), p.getY(), p.getZ());
    }

    public static net.minecraft.core.Direction direction(legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        return facing == null ? null : net.minecraft.core.Direction.from3DDataValue(facing.getIndex());
    }

    public static legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing(net.minecraft.core.Direction direction) {
        return direction == null ? null : legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.byIndex(direction.get3DDataValue());
    }

    public static net.minecraft.world.level.Level level(World world) {
        return world == null ? null : world.mcc$level();
    }

    public static net.minecraft.world.entity.player.Player player(EntityPlayer player) {
        return player == null ? null : player.mcc$player();
    }

    /** {@code EntityPlayer.dropItem(stack, true)}: thrown from the player's eyes as a player-dropped item. */
    public static void dropFromPlayer(EntityPlayer player, ItemStack stack) {
        var p = player(player);
        if (p == null || stack == null || stack.isEmpty()) return;
        p.drop(Stacks1122.toModern(stack), true);
    }

    /** {@code InventoryHelper.spawnItemStack}: the stack scattered in random-sized pieces around the point. */
    public static void scatter(World world, double x, double y, double z, ItemStack stack) {
        var level = level(world);
        if (level == null || stack == null || stack.isEmpty()) return;
        net.minecraft.world.Containers.dropItemStack(level, x, y, z, Stacks1122.toModern(stack));
    }

    /** {@code Block.spawnAsEntity}: one item entity at a random point of the block, when the game rule allows tile drops. */
    public static void popResource(World world, BlockPos pos, ItemStack stack) {
        var level = level(world);
        if (level == null || level.isClientSide() || stack == null || stack.isEmpty()) return;
        net.minecraft.world.level.block.Block.popResource(level, pos(pos), Stacks1122.toModern(stack));
    }

    /** The modern player given to a legacy stack as its holder (its own count was taken). */
    public static void giveToPlayer(EntityPlayer player, ItemStack stack) {
        var p = player(player);
        if (p == null || stack == null || stack.isEmpty()) return;
        var modern = Stacks1122.toModern(stack);
        int before = modern.getCount();
        p.getInventory().add(modern);
        if (modern.getCount() < before) {
            p.level().playSound(null, p.getX(), p.getY() + 0.5, p.getZ(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.2F, ((p.getRandom().nextFloat() - p.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
        }
        if (!modern.isEmpty()) p.drop(modern, false);
        stack.setCount(0);
    }
}
