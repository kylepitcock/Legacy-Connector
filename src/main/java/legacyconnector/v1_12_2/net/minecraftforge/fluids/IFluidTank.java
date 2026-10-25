package legacyconnector.v1_12_2.net.minecraftforge.fluids;

public interface IFluidTank {
    FluidStack getFluid();

    int getFluidAmount();

    int getCapacity();

    FluidTankInfo getInfo();

    int fill(FluidStack resource, boolean doFill);

    FluidStack drain(int maxDrain, boolean doDrain);
}
