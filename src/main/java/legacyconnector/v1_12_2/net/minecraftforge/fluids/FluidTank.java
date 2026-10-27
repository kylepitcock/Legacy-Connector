package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.FluidTankProperties;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidTankProperties;

/** A single-fluid tank. */
public class FluidTank implements IFluidTank, IFluidHandler {
    protected FluidStack fluid;
    protected int capacity;
    protected TileEntity tile;
    protected boolean canFill = true;
    protected boolean canDrain = true;
    protected IFluidTankProperties[] tankProperties;

    public FluidTank(int capacity) {
        this((FluidStack) null, capacity);
    }

    public FluidTank(FluidStack fluidStack, int capacity) {
        this.fluid = fluidStack;
        this.capacity = capacity;
    }

    public FluidTank(Fluid fluid, int amount, int capacity) {
        this(new FluidStack(fluid, amount), capacity);
    }

    public FluidTank readFromNBT(NBTTagCompound nbt) {
        if (!nbt.hasKey("Empty")) {
            FluidStack stack = FluidStack.loadFluidStackFromNBT(nbt);
            setFluid(stack);
        } else {
            setFluid(null);
        }
        return this;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        if (fluid != null) fluid.writeToNBT(nbt);
        else nbt.setString("Empty", "");
        return nbt;
    }

    @Override
    public FluidStack getFluid() {
        return fluid;
    }

    public void setFluid(FluidStack fluid) {
        this.fluid = fluid;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setTileEntity(TileEntity tile) {
        this.tile = tile;
    }

    public TileEntity getTileEntity() {
        return tile;
    }

    @Override
    public int getFluidAmount() {
        return fluid == null ? 0 : fluid.amount;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    public boolean canFill() {
        return canFill;
    }

    public boolean canDrain() {
        return canDrain;
    }

    public void setCanFill(boolean canFill) {
        this.canFill = canFill;
    }

    public void setCanDrain(boolean canDrain) {
        this.canDrain = canDrain;
    }

    public boolean canFillFluidType(FluidStack fluid) {
        return canFill();
    }

    public boolean canDrainFluidType(FluidStack fluid) {
        return fluid != null && canDrain();
    }

    @Override
    public FluidTankInfo getInfo() {
        return new FluidTankInfo(this);
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        if (tankProperties == null) tankProperties = new IFluidTankProperties[] {new FluidTankProperties(fluid, capacity, canFill, canDrain)};
        return new IFluidTankProperties[] {new FluidTankProperties(fluid, capacity, canFill, canDrain)};
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || !canFillFluidType(resource)) return 0;
        return fillInternal(resource, doFill);
    }

    /** Fills ignoring canFill (the tile entity's own filling). */
    public int fillInternal(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;
        if (!doFill) {
            if (fluid == null) return Math.min(capacity, resource.amount);
            if (!fluid.isFluidEqual(resource)) return 0;
            return Math.min(capacity - fluid.amount, resource.amount);
        }
        if (fluid == null) {
            fluid = new FluidStack(resource, Math.min(capacity, resource.amount));
            onContentsChanged();
            return fluid.amount;
        }
        if (!fluid.isFluidEqual(resource)) return 0;
        int filled = capacity - fluid.amount;
        if (resource.amount < filled) {
            fluid.amount += resource.amount;
            filled = resource.amount;
        } else {
            fluid.amount = capacity;
        }
        onContentsChanged();
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || !canDrainFluidType(getFluid())) return null;
        return drainInternal(resource, doDrain);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (!canDrainFluidType(fluid)) return null;
        return drainInternal(maxDrain, doDrain);
    }

    /** Drains ignoring canDrain (the tile entity's own draining). */
    public FluidStack drainInternal(FluidStack resource, boolean doDrain) {
        if (resource == null || !resource.isFluidEqual(fluid)) return null;
        return drainInternal(resource.amount, doDrain);
    }

    public FluidStack drainInternal(int maxDrain, boolean doDrain) {
        if (fluid == null || maxDrain <= 0) return null;
        int drained = Math.min(fluid.amount, maxDrain);
        FluidStack stack = new FluidStack(fluid, drained);
        if (doDrain) {
            fluid.amount -= drained;
            if (fluid.amount <= 0) fluid = null;
            onContentsChanged();
        }
        return stack;
    }

    protected void onContentsChanged() {
    }
}
