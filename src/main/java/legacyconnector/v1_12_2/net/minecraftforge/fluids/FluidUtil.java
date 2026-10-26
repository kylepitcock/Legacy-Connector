package legacyconnector.v1_12_2.net.minecraftforge.fluids;

import legacyconnector.v1_12_2.net.minecraft.init.Items;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.FluidTankProperties;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandlerItem;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidTankProperties;

/** Forge's fluid container helpers: an item's fluid handler (its own capability, or vanilla's buckets) and filled buckets. */
public class FluidUtil {
    /** The fluid handler of one item of the stack (a copy of count 1), or null when the item holds no fluid. */
    public static IFluidHandlerItem getFluidHandler(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        var provider = copy.getItem().initCapabilities(copy, copy.getTagCompound());
        var cap = CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY;
        if (provider != null && cap != null && provider.hasCapability(cap, null)) {
            IFluidHandlerItem handler = provider.getCapability(cap, null);
            if (handler != null) return handler;
        }
        if (copy.getItem() == Items.BUCKET || copy.getItem() == Items.WATER_BUCKET || copy.getItem() == Items.LAVA_BUCKET) return new VanillaBucket(copy);
        return null;
    }

    public static FluidStack getFluidContained(ItemStack container) {
        IFluidHandlerItem handler = getFluidHandler(container);
        return handler == null ? null : handler.drain(Integer.MAX_VALUE, false);
    }

    /** A bucket of the fluid: vanilla's water and lava buckets; other fluids have no bucket item here. */
    public static ItemStack getFilledBucket(FluidStack fluidStack) {
        if (fluidStack == null || fluidStack.getFluid() == null) return ItemStack.EMPTY;
        Fluid fluid = fluidStack.getFluid();
        if (fluid == FluidRegistry.WATER || "water".equals(fluid.getName())) return new ItemStack(Items.WATER_BUCKET);
        if (fluid == FluidRegistry.LAVA || "lava".equals(fluid.getName())) return new ItemStack(Items.LAVA_BUCKET);
        return ItemStack.EMPTY;
    }

    /** Forge's FluidBucketWrapper for the vanilla buckets: empty, water or lava, a bucket (1000 mB) at a time. */
    private static final class VanillaBucket implements IFluidHandlerItem {
        private ItemStack container;

        VanillaBucket(ItemStack container) {
            this.container = container;
        }

        private FluidStack contents() {
            if (container.getItem() == Items.WATER_BUCKET) return new FluidStack(FluidRegistry.WATER, 1000);
            if (container.getItem() == Items.LAVA_BUCKET) return new FluidStack(FluidRegistry.LAVA, 1000);
            return null;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[] {new FluidTankProperties(contents(), 1000)};
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (container.getCount() != 1 || resource == null || resource.amount < 1000 || contents() != null) return 0;
            ItemStack filled = getFilledBucket(resource);
            if (filled.isEmpty()) return 0;
            if (doFill) container = filled;
            return 1000;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            FluidStack contents = contents();
            if (resource == null || contents == null || !contents.isFluidEqual(resource)) return null;
            return drain(resource.amount, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            FluidStack contents = contents();
            if (container.getCount() != 1 || maxDrain < 1000 || contents == null) return null;
            if (doDrain) container = new ItemStack(Items.BUCKET);
            return contents;
        }
    }
}
