package legacyconnector.v1_12_2.net.minecraftforge.fluids;

public final class FluidTankInfo {
    public final FluidStack fluid;
    public final int capacity;

    public FluidTankInfo(FluidStack fluid, int capacity) {
        this.fluid = fluid;
        this.capacity = capacity;
    }

    public FluidTankInfo(IFluidTank tank) {
        this.fluid = tank.getFluid();
        this.capacity = tank.getCapacity();
    }
}
