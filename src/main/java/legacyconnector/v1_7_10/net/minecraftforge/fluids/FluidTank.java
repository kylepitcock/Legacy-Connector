package legacyconnector.v1_7_10.net.minecraftforge.fluids;

import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;

/** Forge 1.7.10 reference tank. */
public class FluidTank implements IFluidTank {
    protected FluidStack fluid;
    protected int capacity;
    protected TileEntity tile;

    public FluidTank(int capacity) {
        this(null, capacity);
    }

    public FluidTank(FluidStack stack, int capacity) {
        this.fluid = stack;
        this.capacity = capacity;
    }

    public FluidTank(Fluid fluid, int amount, int capacity) {
        this(new FluidStack(fluid, amount), capacity);
    }

    public FluidTank readFromNBT(NBTTagCompound nbt) {
        if (!nbt.hasKey("Empty")) {
            FluidStack f = FluidStack.loadFluidStackFromNBT(nbt);
            setFluid(f);
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

    public void setFluid(FluidStack fluid) {
        this.fluid = fluid;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public FluidStack getFluid() {
        return fluid;
    }

    @Override
    public int getFluidAmount() {
        return fluid == null ? 0 : fluid.amount;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public FluidTankInfo getInfo() {
        return new FluidTankInfo(this);
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null) return 0;
        if (fluid == null) {
            int amount = Math.min(capacity, resource.amount);
            if (doFill) fluid = new FluidStack(resource, amount);
            return amount;
        }
        if (!fluid.isFluidEqual(resource)) return 0;
        int filled = Math.min(capacity - fluid.amount, resource.amount);
        if (doFill) fluid.amount += filled;
        return filled;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (fluid == null) return null;
        int drained = Math.min(maxDrain, fluid.amount);
        FluidStack out = new FluidStack(fluid, drained);
        if (doDrain) {
            fluid.amount -= drained;
            if (fluid.amount <= 0) fluid = null;
        }
        return out;
    }
}
