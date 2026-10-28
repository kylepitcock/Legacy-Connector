package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.wrappers;

import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.IFluidBlock;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.FluidTankProperties;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidTankProperties;

/** Forge: a placed fluid block as a fluid handler (draining it removes the block; filling places the fluid there). */
public class FluidBlockWrapper implements IFluidHandler {
    protected final IFluidBlock fluidBlock;
    protected final World world;
    protected final BlockPos blockPos;

    public FluidBlockWrapper(IFluidBlock fluidBlock, World world, BlockPos blockPos) {
        this.fluidBlock = fluidBlock;
        this.world = world;
        this.blockPos = blockPos;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        float percentFilled = fluidBlock.getFilledPercentage(world, blockPos);
        if (percentFilled < 0) percentFilled *= -1;
        int amountFilled = Math.round(1000 * percentFilled);
        FluidStack fluid = amountFilled > 0 ? new FluidStack(fluidBlock.getFluid(), amountFilled) : null;
        return new FluidTankProperties[] {new FluidTankProperties(fluid, 1000, false, true)};
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null) return 0;
        return fluidBlock.place(world, blockPos, resource, doFill);
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || !fluidBlock.canDrain(world, blockPos)) return null;
        FluidStack simulated = fluidBlock.drain(world, blockPos, false);
        if (simulated != null && simulated.amount <= resource.amount && resource.isFluidEqual(simulated)) {
            if (doDrain) return fluidBlock.drain(world, blockPos, true);
            return simulated;
        }
        return null;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0 || !fluidBlock.canDrain(world, blockPos)) return null;
        FluidStack simulated = fluidBlock.drain(world, blockPos, false);
        if (simulated != null && simulated.amount <= maxDrain) {
            if (doDrain) return fluidBlock.drain(world, blockPos, true);
            return simulated;
        }
        return null;
    }
}
