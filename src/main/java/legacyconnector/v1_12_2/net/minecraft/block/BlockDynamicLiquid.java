package legacyconnector.v1_12_2.net.minecraft.block;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2's flowing water and lava: each update the level follows the neighbours (two water sources make a third), the liquid falls,
 * then spreads sideways towards the nearest drop within four blocks, and settles into the still block. Vanilla's own liquids flow
 * as modern fluids; this runs for mods' liquids built on it.
 */
public class BlockDynamicLiquid extends BlockLiquid {
    int adjacentSourceBlocks;

    protected BlockDynamicLiquid(Material materialIn) {
        super(materialIn);
    }

    private void placeStaticBlock(World worldIn, BlockPos pos, IBlockState currentState) {
        worldIn.setBlockState(pos, getStaticBlock(material).getDefaultState().withProperty(LEVEL, currentState.getValue(LEVEL)), 2);
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (!worldIn.isAreaLoaded(pos, reduceAmountFor(worldIn))) return;
        int level = state.getValue(LEVEL);
        int step = material == Material.LAVA && !(World.mcc$get$provider(worldIn) != null && World.mcc$get$provider(worldIn).isNether()) ? 2 : 1;
        int rate = tickRate(worldIn);
        if (level > 0) {
            int lowest = -100;
            adjacentSourceBlocks = 0;
            for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) lowest = checkAdjacentBlock(worldIn, pos.offset(f), lowest);
            int next = lowest + step;
            if (next >= 8 || lowest < 0) next = -1;
            int above = getDepth(worldIn.getBlockState(pos.up()));
            if (above >= 0) next = above >= 8 ? above : above + 8;
            if (adjacentSourceBlocks >= 2 && material == Material.WATER) {
                IBlockState below = worldIn.getBlockState(pos.down());
                if (below.getMaterial().isSolid()) next = 0;
                else if (below.getMaterial() == material && below.getValue(LEVEL) == 0) next = 0;
            }
            if (material == Material.LAVA && level < 8 && next < 8 && next > level && rand.nextInt(4) != 0) rate *= 4;
            if (next == level) {
                placeStaticBlock(worldIn, pos, state);
            } else {
                level = next;
                if (next < 0) {
                    worldIn.setBlockToAir(pos);
                } else {
                    state = state.withProperty(LEVEL, next);
                    worldIn.setBlockState(pos, state, 2);
                    worldIn.scheduleUpdate(pos, this, rate);
                    worldIn.notifyNeighborsOfStateChange(pos, this, false);
                }
            }
        } else {
            placeStaticBlock(worldIn, pos, state);
        }
        IBlockState below = worldIn.getBlockState(pos.down());
        if (canFlowInto(worldIn, pos.down(), below)) {
            if (material == Material.LAVA && worldIn.getBlockState(pos.down()).getMaterial() == Material.WATER) {
                worldIn.setBlockState(pos.down(), legacyconnector.v1_12_2.net.minecraft.init.Blocks.STONE.getDefaultState());
                triggerMixEffects(worldIn, pos.down());
                return;
            }
            tryFlowInto(worldIn, pos.down(), below, level >= 8 ? level : level + 8);
        } else if (level >= 0 && (level == 0 || isBlocked(worldIn, pos.down(), below))) {
            int side = level >= 8 ? 1 : level + step;
            if (side >= 8) return;
            for (EnumFacing f : getPossibleFlowDirections(worldIn, pos)) tryFlowInto(worldIn, pos.offset(f), worldIn.getBlockState(pos.offset(f)), side);
        }
    }

    private static int reduceAmountFor(World worldIn) {
        return 1;
    }

    private void tryFlowInto(World worldIn, BlockPos pos, IBlockState state, int level) {
        if (!canFlowInto(worldIn, pos, state)) return;
        if (state.getMaterial() != Material.AIR) {
            if (material == Material.LAVA) triggerMixEffects(worldIn, pos);
            else if (state.getBlock() != legacyconnector.v1_12_2.net.minecraft.init.Blocks.SNOW_LAYER) state.getBlock().dropBlockAsItem(worldIn, pos, state, 0);
        }
        worldIn.setBlockState(pos, getDefaultState().withProperty(LEVEL, level), 3);
    }

    private int getSlopeDistance(World worldIn, BlockPos pos, int distance, EnumFacing calculateFlowCost) {
        int best = 1000;
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            if (f == calculateFlowCost) continue;
            BlockPos p = pos.offset(f);
            IBlockState s = worldIn.getBlockState(p);
            if (isBlocked(worldIn, p, s) || s.getMaterial() == material && s.getValue(LEVEL) <= 0) continue;
            if (!isBlocked(worldIn, p.down(), worldIn.getBlockState(p.down()))) return distance;
            if (distance < getSlopeFindDistance(worldIn)) {
                int d = getSlopeDistance(worldIn, p, distance + 1, f.getOpposite());
                if (d < best) best = d;
            }
        }
        return best;
    }

    private int getSlopeFindDistance(World worldIn) {
        return material == Material.LAVA && !(World.mcc$get$provider(worldIn) != null && World.mcc$get$provider(worldIn).isNether()) ? 2 : 4;
    }

    private Set<EnumFacing> getPossibleFlowDirections(World worldIn, BlockPos pos) {
        int best = 1000;
        Set<EnumFacing> out = EnumSet.noneOf(EnumFacing.class);
        for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
            BlockPos p = pos.offset(f);
            IBlockState s = worldIn.getBlockState(p);
            if (isBlocked(worldIn, p, s) || s.getMaterial() == material && s.getValue(LEVEL) <= 0) continue;
            int d = isBlocked(worldIn, p.down(), worldIn.getBlockState(p.down())) ? getSlopeDistance(worldIn, p, 1, f.getOpposite()) : 0;
            if (d < best) out.clear();
            if (d <= best) {
                out.add(f);
                best = d;
            }
        }
        return out;
    }

    private boolean isBlocked(World worldIn, BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        Material m = state.getMaterial();
        if (!(block instanceof BlockDoor) && !Block.mcc$named(block, "standing_sign", "wall_sign", "ladder", "reeds")) {
            return m != Material.PORTAL && m != Material.STRUCTURE_VOID && m.blocksMovement();
        }
        return true;
    }

    protected int checkAdjacentBlock(World worldIn, BlockPos pos, int currentMinLevel) {
        int i = getDepth(worldIn.getBlockState(pos));
        if (i < 0) return currentMinLevel;
        if (i == 0) ++adjacentSourceBlocks;
        if (i >= 8) i = 0;
        return currentMinLevel >= 0 && i >= currentMinLevel ? currentMinLevel : i;
    }

    private boolean canFlowInto(World worldIn, BlockPos pos, IBlockState state) {
        Material m = state.getMaterial();
        return m != material && m != Material.LAVA && !isBlocked(worldIn, pos, state);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (!checkForMixing(worldIn, pos, state)) worldIn.scheduleUpdate(pos, this, tickRate(worldIn));
    }
}
