package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability;

import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;

public interface IFluidTankProperties {
    FluidStack getContents();

    int getCapacity();

    boolean canFill();

    boolean canDrain();

    boolean canFillFluidType(FluidStack fluidStack);

    boolean canDrainFluidType(FluidStack fluidStack);
}
