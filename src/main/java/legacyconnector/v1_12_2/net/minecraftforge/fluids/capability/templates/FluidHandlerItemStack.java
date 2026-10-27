package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.templates;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.FluidTankProperties;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandlerItem;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidTankProperties;

/** A fluid container item holding its fluid in the stack's "Fluid" tag, any amount up to the capacity (Forge's template). */
public class FluidHandlerItemStack implements IFluidHandlerItem, ICapabilityProvider {
    public static final String FLUID_NBT_KEY = "Fluid";

    protected ItemStack container;
    protected int capacity;

    public FluidHandlerItemStack(ItemStack container, int capacity) {
        this.container = container;
        this.capacity = capacity;
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    public FluidStack getFluid() {
        NBTTagCompound tag = container.getTagCompound();
        if (tag == null || !tag.hasKey(FLUID_NBT_KEY)) return null;
        return FluidStack.loadFluidStackFromNBT(tag.getCompoundTag(FLUID_NBT_KEY));
    }

    protected void setFluid(FluidStack fluid) {
        if (!container.hasTagCompound()) container.setTagCompound(new NBTTagCompound());
        NBTTagCompound fluidTag = new NBTTagCompound();
        fluid.writeToNBT(fluidTag);
        container.getTagCompound().setTag(FLUID_NBT_KEY, fluidTag);
        container.setTagCompound(container.getTagCompound());
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new IFluidTankProperties[] {new FluidTankProperties(getFluid(), capacity)};
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (container.getCount() != 1 || resource == null || resource.amount <= 0 || !canFillFluidType(resource)) return 0;
        FluidStack contained = getFluid();
        if (contained == null) {
            int fillAmount = Math.min(capacity, resource.amount);
            if (doFill) {
                FluidStack filled = resource.copy();
                filled.amount = fillAmount;
                setFluid(filled);
            }
            return fillAmount;
        }
        if (contained.isFluidEqual(resource)) {
            int fillAmount = Math.min(capacity - contained.amount, resource.amount);
            if (doFill && fillAmount > 0) {
                contained.amount += fillAmount;
                setFluid(contained);
            }
            return fillAmount;
        }
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (container.getCount() != 1 || resource == null || resource.amount <= 0 || !resource.isFluidEqual(getFluid())) return null;
        return drain(resource.amount, doDrain);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (container.getCount() != 1 || maxDrain <= 0) return null;
        FluidStack contained = getFluid();
        if (contained == null || contained.amount <= 0 || !canDrainFluidType(contained)) return null;
        int drainAmount = Math.min(contained.amount, maxDrain);
        FluidStack drained = contained.copy();
        drained.amount = drainAmount;
        if (doDrain) {
            contained.amount -= drainAmount;
            if (contained.amount == 0) setContainerToEmpty();
            else setFluid(contained);
        }
        return drained;
    }

    public boolean canFillFluidType(FluidStack fluid) {
        return true;
    }

    public boolean canDrainFluidType(FluidStack fluid) {
        return true;
    }

    protected void setContainerToEmpty() {
        NBTTagCompound tag = container.getTagCompound();
        if (tag != null) {
            tag.removeTag(FLUID_NBT_KEY);
            container.setTagCompound(tag.isEmpty() ? null : tag);
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability != null && capability == CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return hasCapability(capability, facing) ? (T) this : null;
    }

    /** Destroyed when emptied (Forge's Consumable variant). */
    public static class Consumable extends FluidHandlerItemStack {
        public Consumable(ItemStack container, int capacity) {
            super(container, capacity);
        }

        @Override
        protected void setContainerToEmpty() {
            super.setContainerToEmpty();
            container.shrink(1);
        }
    }

    /** Swapped for another item when emptied. */
    public static class SwapEmpty extends FluidHandlerItemStack {
        protected final ItemStack emptyContainer;

        public SwapEmpty(ItemStack container, ItemStack emptyContainer, int capacity) {
            super(container, capacity);
            this.emptyContainer = emptyContainer;
        }

        @Override
        protected void setContainerToEmpty() {
            super.setContainerToEmpty();
            container = emptyContainer;
        }
    }
}
