package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability;

import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidTank;

/** A tank's live properties (contents and rules read from the tank each time). */
public class FluidTankPropertiesWrapper implements IFluidTankProperties {
    protected final FluidTank tank;

    public FluidTankPropertiesWrapper(FluidTank tank) {
        this.tank = tank;
    }

    @Override
    public FluidStack getContents() {
        FluidStack contents = tank.getFluid();
        return contents == null ? null : contents.copy();
    }

    @Override
    public int getCapacity() {
        return tank.getCapacity();
    }

    @Override
    public boolean canFill() {
        return tank.canFill();
    }

    @Override
    public boolean canDrain() {
        return tank.canDrain();
    }

    @Override
    public boolean canFillFluidType(FluidStack fluidStack) {
        return tank.canFillFluidType(fluidStack);
    }

    @Override
    public boolean canDrainFluidType(FluidStack fluidStack) {
        return tank.canDrainFluidType(fluidStack);
    }
}
