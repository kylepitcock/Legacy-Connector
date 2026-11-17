package legacyconnector.v1_20_1.net.minecraftforge.fluids.capability.wrappers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Forge 1.20.1's {@code FluidBlockWrapper}: a placed fluid block as a one-tank handler (a source block drains as one bucket, filling places the source
 * block); 1.21.1 builds these through {@code FluidUtil} and no longer ships the class.
 */
public class FluidBlockWrapper implements IFluidHandler {
    protected final LiquidBlock fluidBlock;
    protected final Level world;
    protected final BlockPos blockPos;

    public FluidBlockWrapper(LiquidBlock fluidBlock, Level world, BlockPos blockPos) {
        this.fluidBlock = fluidBlock;
        this.world = world;
        this.blockPos = blockPos;
    }

    /** The source fluid of the block (its default state is the source level). */
    private net.minecraft.world.level.material.Fluid sourceFluid() {
        return fluidBlock.defaultBlockState().getFluidState().getType();
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return FluidType.BUCKET_VOLUME;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.getAmount() < FluidType.BUCKET_VOLUME) return 0;
        if (resource.getFluid() != sourceFluid()) return 0;
        if (action.execute()) {
            world.setBlock(blockPos, fluidBlock.defaultBlockState(), 11);
        }
        return FluidType.BUCKET_VOLUME;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || resource.getFluid() != sourceFluid()) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain < FluidType.BUCKET_VOLUME) return FluidStack.EMPTY;
        FluidState state = world.getFluidState(blockPos);
        if (!state.isSource() || state.getType() != sourceFluid()) return FluidStack.EMPTY;
        FluidStack stack = new FluidStack(state.getType(), FluidType.BUCKET_VOLUME);
        if (action.execute()) world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 11);
        return stack;
    }
}
