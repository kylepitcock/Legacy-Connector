package legacyconnector.v1_12_2.net.minecraft.block.state;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.block.BlockPistonBase;
import legacyconnector.v1_12_2.net.minecraft.block.material.EnumPushReaction;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2's piston push planner: the blocks a piston at a position would move (at most twelve, slime blocks pulling their neighbours
 * along) and the ones it would break.
 */
public class BlockPistonStructureHelper {
    private final World world;
    private final BlockPos pistonPos;
    private final BlockPos blockToMove;
    private final EnumFacing moveDirection;
    private final List<BlockPos> toMove = new ArrayList<>();
    private final List<BlockPos> toDestroy = new ArrayList<>();

    public BlockPistonStructureHelper(World worldIn, BlockPos posIn, EnumFacing pistonFacing, boolean extending) {
        this.world = worldIn;
        this.pistonPos = posIn;
        if (extending) {
            this.moveDirection = pistonFacing;
            this.blockToMove = posIn.offset(pistonFacing);
        } else {
            this.moveDirection = pistonFacing.getOpposite();
            this.blockToMove = posIn.offset(pistonFacing, 2);
        }
    }

    public boolean canMove() {
        toMove.clear();
        toDestroy.clear();
        IBlockState state = world.getBlockState(blockToMove);
        if (!BlockPistonBase.canPush(state, world, blockToMove, moveDirection, false, moveDirection)) {
            if (state.getPushReaction() == EnumPushReaction.DESTROY) {
                toDestroy.add(blockToMove);
                return true;
            }
            return false;
        }
        if (!addBlockLine(blockToMove, moveDirection)) return false;
        for (int i = 0; i < toMove.size(); ++i) {
            BlockPos p = toMove.get(i);
            if (isSticky(world.getBlockState(p)) && !addBranchingBlocks(p)) return false;
        }
        return true;
    }

    private static boolean isSticky(IBlockState state) {
        return Block.mcc$named(state.getBlock(), "slime");
    }

    private boolean addBlockLine(BlockPos origin, EnumFacing direction) {
        IBlockState state = world.getBlockState(origin);
        if (state.getBlock().isAir(state, world, origin)) return true;
        if (!BlockPistonBase.canPush(state, world, origin, moveDirection, false, direction)) return true;
        if (origin.equals(pistonPos)) return true;
        if (toMove.contains(origin)) return true;
        int length = 1;
        if (length + toMove.size() > 12) return false;
        while (isSticky(state)) {
            BlockPos p = origin.offset(moveDirection.getOpposite(), length);
            state = world.getBlockState(p);
            if (state.getBlock().isAir(state, world, p) || !BlockPistonBase.canPush(state, world, p, moveDirection, false, moveDirection.getOpposite())
                    || p.equals(pistonPos)) {
                break;
            }
            ++length;
            if (length + toMove.size() > 12) return false;
        }
        int added = 0;
        for (int i = length - 1; i >= 0; --i) {
            toMove.add(origin.offset(moveDirection.getOpposite(), i));
            ++added;
        }
        int step = 1;
        while (true) {
            BlockPos next = origin.offset(moveDirection, step);
            int index = toMove.indexOf(next);
            if (index > -1) {
                reorderListAtCollision(added, index);
                for (int k = 0; k <= index + added; ++k) {
                    BlockPos p = toMove.get(k);
                    if (isSticky(world.getBlockState(p)) && !addBranchingBlocks(p)) return false;
                }
                return true;
            }
            state = world.getBlockState(next);
            if (state.getBlock().isAir(state, world, next)) return true;
            if (!BlockPistonBase.canPush(state, world, next, moveDirection, true, moveDirection) || next.equals(pistonPos)) return false;
            if (state.getPushReaction() == EnumPushReaction.DESTROY) {
                toDestroy.add(next);
                return true;
            }
            if (toMove.size() >= 12) return false;
            toMove.add(next);
            ++added;
            ++step;
        }
    }

    private void reorderListAtCollision(int p_177255_1_, int p_177255_2_) {
        List<BlockPos> head = new ArrayList<>(toMove.subList(0, p_177255_2_));
        List<BlockPos> tail = new ArrayList<>(toMove.subList(toMove.size() - p_177255_1_, toMove.size()));
        List<BlockPos> middle = new ArrayList<>(toMove.subList(p_177255_2_, toMove.size() - p_177255_1_));
        toMove.clear();
        toMove.addAll(head);
        toMove.addAll(tail);
        toMove.addAll(middle);
    }

    private boolean addBranchingBlocks(BlockPos fromPos) {
        for (EnumFacing f : EnumFacing.values()) {
            if (f.getAxis() != moveDirection.getAxis() && !addBlockLine(fromPos.offset(f), f)) return false;
        }
        return true;
    }

    public List<BlockPos> getBlocksToMove() {
        return toMove;
    }

    public List<BlockPos> getBlocksToDestroy() {
        return toDestroy;
    }
}
