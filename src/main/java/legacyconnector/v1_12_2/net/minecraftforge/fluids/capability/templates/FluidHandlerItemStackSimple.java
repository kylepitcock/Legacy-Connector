package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.templates;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;

/** A container that is either empty or full: it fills and drains its whole capacity at once (buckets, cells). */
public class FluidHandlerItemStackSimple extends FluidHandlerItemStack {
    public FluidHandlerItemStackSimple(ItemStack container, int capacity) {
        super(container, capacity);
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (container.getCount() != 1 || resource == null || resource.amount <= 0 || !canFillFluidType(resource)) return 0;
        if (getFluid() == null && resource.amount >= capacity) {
            if (doFill) {
                FluidStack filled = resource.copy();
                filled.amount = capacity;
                setFluid(filled);
            }
            return capacity;
        }
        return 0;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (container.getCount() != 1 || maxDrain <= 0) return null;
        FluidStack contained = getFluid();
        if (contained == null || contained.amount <= 0 || !canDrainFluidType(contained)) return null;
        if (maxDrain < contained.amount) return null;
        if (doDrain) setContainerToEmpty();
        return contained.copy();
    }

    public static class Consumable extends FluidHandlerItemStackSimple {
        public Consumable(ItemStack container, int capacity) {
            super(container, capacity);
        }

        @Override
        protected void setContainerToEmpty() {
            super.setContainerToEmpty();
            container.shrink(1);
        }
    }

    public static class SwapEmpty extends FluidHandlerItemStackSimple {
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
