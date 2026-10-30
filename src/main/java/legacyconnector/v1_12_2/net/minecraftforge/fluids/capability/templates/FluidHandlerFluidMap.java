package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidTankProperties;

/** Forge: one fluid handler per fluid (a barrel with an input and an output tank); fills and drains go to the handler of the fluid. */
public class FluidHandlerFluidMap implements IFluidHandler {
    protected final Map<Fluid, IFluidHandler> handlers;

    public FluidHandlerFluidMap() {
        this(new HashMap<>());
    }

    public FluidHandlerFluidMap(Map<Fluid, IFluidHandler> handlers) {
        this.handlers = handlers;
    }

    public FluidHandlerFluidMap addHandler(Fluid fluid, IFluidHandler handler) {
        handlers.put(fluid, handler);
        return this;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        List<IFluidTankProperties> all = new ArrayList<>();
        for (IFluidHandler h : handlers.values()) Collections.addAll(all, h.getTankProperties());
        return all.toArray(new IFluidTankProperties[0]);
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null) return 0;
        IFluidHandler handler = handlers.get(resource.getFluid());
        return handler == null ? 0 : handler.fill(resource, doFill);
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null) return null;
        IFluidHandler handler = handlers.get(resource.getFluid());
        return handler == null ? null : handler.drain(resource, doDrain);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        for (IFluidHandler handler : handlers.values()) {
            FluidStack drained = handler.drain(maxDrain, doDrain);
            if (drained != null) return drained;
        }
        return null;
    }
}
