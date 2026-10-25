package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** An amount of a fluid (in millibuckets) with an optional tag. */
public class FluidStack {
    public int amount;
    public NBTTagCompound tag;
    private final Fluid fluid;

    public FluidStack(Fluid fluid, int amount) {
        if (fluid == null) throw new IllegalArgumentException("Fluid cannot be null");
        this.fluid = fluid;
        this.amount = amount;
    }

    public FluidStack(Fluid fluid, int amount, NBTTagCompound nbt) {
        this(fluid, amount);
        if (nbt != null) this.tag = nbt.copy();
    }

    public FluidStack(FluidStack stack, int amount) {
        this(stack.getFluid(), amount, stack.tag);
    }

    public final Fluid getFluid() {
        return fluid;
    }

    public String getLocalizedName() {
        return fluid.getLocalizedName(this);
    }

    public String getUnlocalizedName() {
        return fluid.getUnlocalizedName(this);
    }

    public FluidStack copy() {
        return new FluidStack(fluid, amount, tag);
    }

    public boolean isFluidEqual(FluidStack other) {
        return other != null && other.fluid == fluid && isFluidStackTagEqual(other);
    }

    private boolean isFluidStackTagEqual(FluidStack other) {
        return tag == null ? other.tag == null : other.tag != null && tag.equals(other.tag);
    }

    public boolean containsFluid(FluidStack other) {
        return isFluidEqual(other) && amount >= other.amount;
    }

    public boolean isFluidStackIdentical(FluidStack other) {
        return isFluidEqual(other) && amount == other.amount;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setString("FluidName", fluid.getName());
        nbt.setInteger("Amount", amount);
        if (tag != null) nbt.setTag("Tag", tag);
        return nbt;
    }

    public static FluidStack loadFluidStackFromNBT(NBTTagCompound nbt) {
        if (nbt == null || !nbt.hasKey("FluidName", 8)) return null;
        Fluid fluid = FluidRegistry.getFluid(nbt.getString("FluidName"));
        if (fluid == null) return null;
        FluidStack stack = new FluidStack(fluid, nbt.getInteger("Amount"));
        if (nbt.hasKey("Tag")) stack.tag = nbt.getCompoundTag("Tag");
        return stack;
    }

    @Override
    public final int hashCode() {
        return fluid.hashCode() + amount;
    }

    @Override
    public final boolean equals(Object o) {
        return o instanceof FluidStack s && isFluidStackIdentical(s);
    }
}
