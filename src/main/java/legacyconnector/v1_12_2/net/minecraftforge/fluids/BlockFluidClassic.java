package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * Forge 1.12.2's flowing fluid block: source blocks (level 0) spread with rising levels like water, down first (up for gases), then
 * along the shortest path to a drop; two sources next to each other make a new one when the fluid allows it ({@link #canCreateSources}).
 */
public class BlockFluidClassic extends BlockFluidBase {
    protected static final List<EnumFacing> SIDES = ImmutableList.of(EnumFacing.WEST, EnumFacing.EAST, EnumFacing.NORTH, EnumFacing.SOUTH);
    protected boolean[] isOptimalFlowDirection = new boolean[4];
    protected int[] flowCost = new int[4];
    protected boolean canCreateSources = false;
    /** What draining a source block of this fluid gives (a bucket's worth by default). */
    protected FluidStack stack;

    public BlockFluidClassic(Fluid fluid, Material material) {
        super(fluid, material);
        this.stack = new FluidStack(fluid, Fluid.BUCKET_VOLUME);
    }

    public BlockFluidClassic(Fluid fluid, Material material, legacyconnector.v1_12_2.net.minecraft.block.material.MapColor mapColor) {
        super(fluid, material, mapColor);
        this.stack = new FluidStack(fluid, Fluid.BUCKET_VOLUME);
    }

    @Override
    public BlockFluidClassic setFluidStack(FluidStack stack) {
        this.stack = stack;
        return this;
    }

    public BlockFluidClassic setFluidStackAmount(int amount) {
        if (stack != null) this.stack = new FluidStack(stack.getFluid(), amount);
        return this;
    }

    public FluidStack mcc$stack() {
        return stack;
    }

    @Override
    public int getQuantaValue(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == legacyconnector.v1_12_2.net.minecraft.init.Blocks.AIR) return 0;
        if (state.getBlock() != this) return -1;
        return quantaPerBlock - state.getValue(LEVEL);
    }

    @Override
    public boolean canCollideCheck(IBlockState state, boolean fullHit) {
        return fullHit && state.getValue(LEVEL) == 0;
    }

    @Override
    public int getMaxRenderHeightMeta() {
        return 0;
    }

    /** One flow step: the level follows the neighbours, then the fluid moves down (or up) or outwards. */
    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        int quantaRemaining = quantaPerBlock - state.getValue(LEVEL);
        int expQuanta;
        if (quantaRemaining < quantaPerBlock) {
            int adjacentSourceBlocks = 0;
            if (canCreateSources) {
                for (EnumFacing side : SIDES) if (isSourceBlock(world, pos.offset(side))) adjacentSourceBlocks++;
            }
            if (adjacentSourceBlocks >= 2 && (world.getBlockState(pos.up(densityDir)).getMaterial().isSolid() || isSourceBlock(world, pos.up(densityDir)))) {
                expQuanta = quantaPerBlock;
            } else if (hasVerticalFlow(world, pos)) {
                expQuanta = quantaPerBlock - 1;
            } else {
                int maxQuanta = -100;
                for (EnumFacing side : SIDES) maxQuanta = getLargerQuanta(world, pos.offset(side), maxQuanta);
                expQuanta = maxQuanta - 1;
            }
            if (expQuanta != quantaRemaining) {
                quantaRemaining = expQuanta;
                if (expQuanta <= 0) {
                    world.setBlockToAir(pos);
                } else {
                    world.setBlockState(pos, state.withProperty(LEVEL, quantaPerBlock - expQuanta), 2);
                    world.scheduleUpdate(pos, this, tickRate);
                    world.notifyNeighborsOfStateChange(pos, this, false);
                }
            }
        }
        if (canDisplace(world, pos.up(densityDir))) {
            flowIntoBlock(world, pos.up(densityDir), 1);
            return;
        }
        int flowMeta = quantaPerBlock - quantaRemaining + 1;
        if (flowMeta >= quantaPerBlock) return;
        if (isSourceBlock(world, pos) || !isFlowingVertically(world, pos)) {
            if (hasVerticalFlow(world, pos)) flowMeta = 1;
            boolean[] flowTo = getOptimalFlowDirections(world, pos);
            for (int i = 0; i < 4; i++) if (flowTo[i]) flowIntoBlock(world, pos.offset(SIDES.get(i)), flowMeta);
        }
    }

    protected boolean hasVerticalFlow(IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos.down(densityDir)).getBlock() == this;
    }

    public boolean isFlowingVertically(IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos.up(densityDir)).getBlock() == this
                || world.getBlockState(pos).getBlock() == this && canFlowInto(world, pos.up(densityDir));
    }

    /** A block of this fluid at level 0. */
    public boolean isSourceBlock(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() == this && state.getValue(LEVEL) == 0;
    }

    /** The horizontal directions (west, east, north, south) with the cheapest path to a place the fluid can fall. */
    protected boolean[] getOptimalFlowDirections(World world, BlockPos pos) {
        for (int side = 0; side < 4; side++) {
            flowCost[side] = 1000;
            BlockPos pos2 = pos.offset(SIDES.get(side));
            if (!canFlowInto(world, pos2) || isSourceBlock(world, pos2)) continue;
            if (canFlowInto(world, pos2.up(densityDir))) flowCost[side] = 0;
            else flowCost[side] = calculateFlowCost(world, pos2, 1, side);
        }
        int min = flowCost[0];
        for (int side = 1; side < 4; side++) if (flowCost[side] < min) min = flowCost[side];
        for (int side = 0; side < 4; side++) isOptimalFlowDirection[side] = flowCost[side] == min;
        return isOptimalFlowDirection;
    }

    protected int calculateFlowCost(World world, BlockPos pos, int recurseDepth, int side) {
        int cost = 1000;
        for (int adjSide = 0; adjSide < 4; adjSide++) {
            if (adjSide == 0 && side == 1 || adjSide == 1 && side == 0 || adjSide == 2 && side == 3 || adjSide == 3 && side == 2) continue;
            BlockPos pos2 = pos.offset(SIDES.get(adjSide));
            if (!canFlowInto(world, pos2) || isSourceBlock(world, pos2)) continue;
            if (canFlowInto(world, pos2.up(densityDir))) return recurseDepth;
            if (recurseDepth >= quantaPerBlock / 2) continue;
            cost = Math.min(cost, calculateFlowCost(world, pos2, recurseDepth + 1, adjSide));
        }
        return cost;
    }

    protected void flowIntoBlock(World world, BlockPos pos, int meta) {
        if (meta < 0) return;
        if (displaceIfPossible(world, pos)) world.setBlockState(pos, getDefaultState().withProperty(LEVEL, meta), 3);
    }

    protected boolean canFlowInto(IBlockAccess world, BlockPos pos) {
        if (world.isAirBlock(pos)) return true;
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == this) return true;
        if (displacements.containsKey(state.getBlock())) return displacements.get(state.getBlock());
        Material material = state.getMaterial();
        if (material.blocksMovement() || material == Material.WATER || material == Material.LAVA || material == Material.PORTAL) return false;
        int otherDensity = getDensity(world, pos);
        if (otherDensity == Integer.MAX_VALUE) return true;
        return this.density > otherDensity;
    }

    protected int getLargerQuanta(IBlockAccess world, BlockPos pos, int compare) {
        int quantaRemaining = getQuantaValue(world, pos);
        if (quantaRemaining <= 0) return compare;
        return Math.max(quantaRemaining, compare);
    }

    @Override
    public FluidStack drain(World world, BlockPos pos, boolean doDrain) {
        if (!isSourceBlock(world, pos)) return null;
        if (doDrain) world.setBlockToAir(pos);
        return stack == null ? null : stack.copy();
    }

    @Override
    public boolean canDrain(World world, BlockPos pos) {
        return isSourceBlock(world, pos);
    }

    /** Places a source block when given at least a bucket. */
    @Override
    public int place(World world, BlockPos pos, FluidStack fluidStack, boolean doPlace) {
        if (fluidStack == null || fluidStack.amount < Fluid.BUCKET_VOLUME) return 0;
        if (doPlace) world.setBlockState(pos, getDefaultState(), 11);
        return Fluid.BUCKET_VOLUME;
    }
}
