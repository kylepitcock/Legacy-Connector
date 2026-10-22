package legacyconnector.v1_7_10.net.minecraftforge.fluids;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/** An amount (mB) of a fluid, with optional NBT. NBT format matches Forge 1.7.10 ("FluidName", "Amount", "Tag"). */
public class FluidStack {
    public final Fluid fluid;
    public int amount;
    public NBTTagCompound tag;

    public FluidStack(Fluid fluid, int amount) {
        this.fluid = fluid;
        this.amount = amount;
    }

    public FluidStack(Fluid fluid, int amount, NBTTagCompound nbt) {
        this(fluid, amount);
        if (nbt != null) this.tag = (NBTTagCompound) nbt.copy();
    }

    public FluidStack(FluidStack stack, int amount) {
        this(stack.getFluid(), amount, stack.tag);
    }

    public FluidStack(int fluidID, int amount) {
        this(FluidRegistry.getFluid(fluidID), amount);
    }

    public FluidStack(int fluidID, int amount, NBTTagCompound nbt) {
        this(FluidRegistry.getFluid(fluidID), amount, nbt);
    }

    public static FluidStack loadFluidStackFromNBT(NBTTagCompound nbt) {
        if (nbt == null) return null;
        String name = nbt.getString("FluidName");
        if (name == null || FluidRegistry.getFluid(name) == null) return null;
        FluidStack stack = new FluidStack(FluidRegistry.getFluid(name), nbt.getInteger("Amount"));
        if (nbt.hasKey("Tag")) stack.tag = nbt.getCompoundTag("Tag");
        return stack;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setString("FluidName", FluidRegistry.getFluidName(getFluid()));
        nbt.setInteger("Amount", amount);
        if (tag != null) nbt.setTag("Tag", tag);
        return nbt;
    }

    public final Fluid getFluid() {
        return fluid;
    }

    public final int getFluidID() {
        return FluidRegistry.getFluidID(fluid);
    }

    public String getLocalizedName() {
        return fluid == null ? "" : fluid.getLocalizedName(this);
    }

    public String getUnlocalizedName() {
        return fluid == null ? "" : fluid.getUnlocalizedName(this);
    }

    public FluidStack copy() {
        return new FluidStack(getFluid(), amount, tag);
    }

    public boolean isFluidEqual(FluidStack other) {
        return other != null && getFluid() == other.getFluid() && areFluidStackTagsEqual(this, other);
    }

    public static boolean areFluidStackTagsEqual(FluidStack a, FluidStack b) {
        return a == null && b == null || a != null && b != null && (a.tag == null && b.tag == null || a.tag != null && a.tag.equals(b.tag));
    }

    public boolean containsFluid(FluidStack other) {
        return isFluidEqual(other) && amount >= other.amount;
    }

    public boolean isFluidStackIdentical(FluidStack other) {
        return isFluidEqual(other) && amount == other.amount;
    }

    public boolean isFluidEqual(ItemStack other) {
        return other != null && isFluidEqual(FluidContainerRegistry.getFluidForFilledItem(other));
    }

    @Override
    public final int hashCode() {
        return getFluidID();
    }

    @Override
    public final boolean equals(Object o) {
        return o instanceof FluidStack s && isFluidEqual(s);
    }
}
