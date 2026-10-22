package legacyconnector.v1_7_10.net.minecraftforge.fluids;

import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/** Implemented by tile entities that store fluids. */
public interface IFluidHandler {
    int fill(ForgeDirection from, FluidStack resource, boolean doFill);

    FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain);

    FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain);

    boolean canFill(ForgeDirection from, Fluid fluid);

    boolean canDrain(ForgeDirection from, Fluid fluid);

    FluidTankInfo[] getTankInfo(ForgeDirection from);
}
