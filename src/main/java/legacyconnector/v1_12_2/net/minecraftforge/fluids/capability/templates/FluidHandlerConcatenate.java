package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.templates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidTankProperties;

/** Several fluid handlers as one: fills the first that takes the fluid, drains in order. */
public class FluidHandlerConcatenate implements IFluidHandler {
    protected final IFluidHandler[] subHandlers;

    public FluidHandlerConcatenate(IFluidHandler... fluidHandlers) {
        this.subHandlers = fluidHandlers;
    }

    public FluidHandlerConcatenate(Collection<IFluidHandler> fluidHandlers) {
        this.subHandlers = fluidHandlers.toArray(new IFluidHandler[0]);
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        List<IFluidTankProperties> tanks = new ArrayList<>();
        for (IFluidHandler h : subHandlers) java.util.Collections.addAll(tanks, h.getTankProperties());
        return tanks.toArray(new IFluidTankProperties[0]);
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;
        resource = resource.copy();
        int totalFillAmount = 0;
        for (IFluidHandler handler : subHandlers) {
            int fillAmount = handler.fill(resource, doFill);
            totalFillAmount += fillAmount;
            resource.amount -= fillAmount;
            if (resource.amount <= 0) break;
        }
        return totalFillAmount;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) return null;
        resource = resource.copy();
        FluidStack totalDrained = null;
        for (IFluidHandler handler : subHandlers) {
            FluidStack drain = handler.drain(resource, doDrain);
            if (drain != null) {
                if (totalDrained == null) totalDrained = drain;
                else totalDrained.amount += drain.amount;
                resource.amount -= drain.amount;
                if (resource.amount <= 0) break;
            }
        }
        return totalDrained;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain == 0) return null;
        FluidStack totalDrained = null;
        for (IFluidHandler handler : subHandlers) {
            if (totalDrained == null) {
                totalDrained = handler.drain(maxDrain, doDrain);
                if (totalDrained != null) maxDrain -= totalDrained.amount;
            } else {
                FluidStack copy = totalDrained.copy();
                copy.amount = maxDrain;
                FluidStack drain = handler.drain(copy, doDrain);
                if (drain != null) {
                    totalDrained.amount += drain.amount;
                    maxDrain -= drain.amount;
                }
            }
            if (maxDrain <= 0) break;
        }
        return totalDrained;
    }
}
