package legacyconnector.v1_7_10.net.minecraftforge.common.util;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Forge's rotation of vanilla blocks (wrenches, Botania's wand): a quarter turn around {@code axis} as seen looking along
 * it. Forge did this with per-block metadata tables; here the block's modern state is turned: around the vertical axis by
 * the block's own rotation, around a horizontal axis by turning its six-way facing or its log axis.
 */
public class RotationHelper {
    public static ForgeDirection[] getValidVanillaBlockRotations(Block block) {
        return ForgeDirection.VALID_DIRECTIONS;
    }

    public static boolean rotateVanillaBlock(Block block, World world, int x, int y, int z, ForgeDirection axis) {
        if (world == null || world.isRemote || axis == null || axis == ForgeDirection.UNKNOWN) return false;
        net.minecraft.world.level.Level level = world.mcc$liveLevel();
        if (level == null) return false;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        BlockState turned = turn(state, level, pos, axis);
        if (turned == null || turned == state) return false;
        if (!turned.canSurvive(level, pos)) return false;
        return level.setBlock(pos, turned, 3);
    }

    private static BlockState turn(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, ForgeDirection axis) {
        if (axis == ForgeDirection.UP || axis == ForgeDirection.DOWN) {
            // UP turns clockwise seen from above (north -> east), as Forge's rotation matrix does; DOWN the other way
            return state.rotate(level, pos, axis == ForgeDirection.UP ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90);
        }
        Direction around = axis.mcc$modern();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            ForgeDirection next = ForgeDirection.mcc$of(facing).getRotation(axis);
            return next == ForgeDirection.UNKNOWN ? null : state.setValue(BlockStateProperties.FACING, next.mcc$modern());
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            Direction.Axis current = state.getValue(BlockStateProperties.AXIS);
            if (current == around.getAxis()) return state;
            Direction.Axis other = Direction.Axis.Y == around.getAxis() ? current
                    : current == Direction.Axis.Y ? (around.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X) : Direction.Axis.Y;
            return state.setValue(BlockStateProperties.AXIS, other);
        }
        return null;
    }
}
