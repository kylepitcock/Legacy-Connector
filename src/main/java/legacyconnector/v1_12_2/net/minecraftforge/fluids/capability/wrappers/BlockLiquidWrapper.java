package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.wrappers;

import legacyconnector.v1_12_2.net.minecraft.block.BlockLiquid;
import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.init.Blocks;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.FluidTankProperties;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidTankProperties;

/**
 * Forge: a vanilla liquid block (water, lava) as a fluid handler. A source block drains as one bucket (leaving air); filling with a
 * bucket's worth places a source block of the liquid.
 */
public class BlockLiquidWrapper implements IFluidHandler {
    protected final BlockLiquid blockLiquid;
    protected final World world;
    protected final BlockPos blockPos;

    public BlockLiquidWrapper(BlockLiquid blockLiquid, World world, BlockPos blockPos) {
        this.blockLiquid = blockLiquid;
        this.world = world;
        this.blockPos = blockPos;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        FluidStack contained = null;
        IBlockState state = world.getBlockState(blockPos);
        if (isSource(state)) contained = getStack(state);
        return new FluidTankProperties[] {new FluidTankProperties(contained, Fluid.BUCKET_VOLUME, false, true)};
    }

    /** "Filling" places the liquid: a bucket's worth makes a source block. */
    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount < Fluid.BUCKET_VOLUME) return 0;
        if (doFill) {
            Material material = blockLiquid.getDefaultState().getMaterial();
            BlockLiquid block = BlockLiquid.getFlowingBlock(material);
            world.setBlockState(blockPos, block.getDefaultState().withProperty(BlockLiquid.LEVEL, 0), 11);
        }
        return Fluid.BUCKET_VOLUME;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount < Fluid.BUCKET_VOLUME) return null;
        IBlockState state = world.getBlockState(blockPos);
        if (isSource(state)) {
            FluidStack contained = getStack(state);
            if (contained != null && resource.containsFluid(contained)) {
                if (doDrain) world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 11);
                return contained;
            }
        }
        return null;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain < Fluid.BUCKET_VOLUME) return null;
        IBlockState state = world.getBlockState(blockPos);
        if (isSource(state)) {
            FluidStack contained = getStack(state);
            if (contained != null && doDrain) world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 11);
            return contained;
        }
        return null;
    }

    private boolean isSource(IBlockState state) {
        Material m = state.getMaterial();
        if (m != Material.WATER && m != Material.LAVA) return false;
        return !state.getPropertyKeys().contains(BlockLiquid.LEVEL) || state.getValue(BlockLiquid.LEVEL) == 0;
    }

    private FluidStack getStack(IBlockState state) {
        Material material = state.getMaterial();
        if (material == Material.WATER) return new FluidStack(FluidRegistry.WATER, Fluid.BUCKET_VOLUME);
        if (material == Material.LAVA) return new FluidStack(FluidRegistry.LAVA, Fluid.BUCKET_VOLUME);
        return null;
    }
}
