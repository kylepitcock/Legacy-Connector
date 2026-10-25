package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability;

import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;

public class FluidTankProperties implements IFluidTankProperties {
    private final FluidStack contents;
    private final int capacity;
    private final boolean canFill;
    private final boolean canDrain;

    public FluidTankProperties(FluidStack contents, int capacity) {
        this(contents, capacity, true, true);
    }

    public FluidTankProperties(FluidStack contents, int capacity, boolean canFill, boolean canDrain) {
        this.contents = contents;
        this.capacity = capacity;
        this.canFill = canFill;
        this.canDrain = canDrain;
    }

    @Override
    public FluidStack getContents() {
        return contents == null ? null : contents.copy();
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean canFill() {
        return canFill;
    }

    @Override
    public boolean canDrain() {
        return canDrain;
    }

    @Override
    public boolean canFillFluidType(FluidStack fluidStack) {
        return canFill;
    }

    @Override
    public boolean canDrainFluidType(FluidStack fluidStack) {
        return canDrain;
    }
}
