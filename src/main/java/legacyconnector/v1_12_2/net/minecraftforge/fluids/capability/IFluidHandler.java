package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability;

import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;

public interface IFluidHandler {
    IFluidTankProperties[] getTankProperties();

    int fill(FluidStack resource, boolean doFill);

    FluidStack drain(FluidStack resource, boolean doDrain);

    FluidStack drain(int maxDrain, boolean doDrain);
}
