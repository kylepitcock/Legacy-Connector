package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.templates;

import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.FluidTankProperties;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidTankProperties;

/** A fluid handler with one tank of capacity 0. */
public class EmptyFluidHandler implements IFluidHandler {
    public static final EmptyFluidHandler INSTANCE = new EmptyFluidHandler();
    public static final IFluidTankProperties EMPTY_TANK_PROPERTIES = new FluidTankProperties(null, 0, false, false);
    public static final IFluidTankProperties[] EMPTY_TANK_PROPERTIES_ARRAY = new IFluidTankProperties[] {EMPTY_TANK_PROPERTIES};

    protected EmptyFluidHandler() {
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return EMPTY_TANK_PROPERTIES_ARRAY;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        return null;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        return null;
    }
}
