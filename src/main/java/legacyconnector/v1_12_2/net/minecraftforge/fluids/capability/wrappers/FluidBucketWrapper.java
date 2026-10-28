package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.wrappers;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidRegistry;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidUtil;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.FluidTankProperties;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidHandlerItem;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.IFluidTankProperties;

/** Forge: a bucket as a fluid handler: a bucket's worth (1000 mB) in or out at once, the empty bucket becoming the filled one and back. */
public class FluidBucketWrapper implements IFluidHandlerItem, ICapabilityProvider {
    protected ItemStack container;

    public FluidBucketWrapper(ItemStack container) {
        this.container = container;
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    public boolean canFillFluidType(FluidStack fluid) {
        if (fluid == null || fluid.getFluid() == null) return false;
        return !FluidUtil.getFilledBucket(fluid).isEmpty();
    }

    /** The fluid in the bucket (null for an empty bucket). */
    public FluidStack getFluid() {
        if (container == null || container.isEmpty()) return null;
        var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(container);
        if (modern.getItem() instanceof net.minecraft.world.item.BucketItem bucket) {
            Fluid fluid = BlockWrapper.mcc$legacy(bucket.content);
            if (fluid != null && bucket.content != net.minecraft.world.level.material.Fluids.EMPTY) return new FluidStack(fluid, Fluid.BUCKET_VOLUME);
        }
        if (modern.is(net.minecraft.world.item.Items.MILK_BUCKET)) {
            Fluid milk = FluidRegistry.getFluid("milk");
            if (milk != null) return new FluidStack(milk, Fluid.BUCKET_VOLUME);
        }
        return null;
    }

    protected void setFluid(FluidStack fluidStack) {
        if (fluidStack == null) container = new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.BUCKET);
        else container = FluidUtil.getFilledBucket(fluidStack);
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new FluidTankProperties[] {new FluidTankProperties(getFluid(), Fluid.BUCKET_VOLUME)};
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (container.getCount() != 1 || resource == null || resource.amount < Fluid.BUCKET_VOLUME || getFluid() != null || !canFillFluidType(resource)) return 0;
        if (doFill) setFluid(resource);
        return Fluid.BUCKET_VOLUME;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (container.getCount() != 1 || resource == null || resource.amount < Fluid.BUCKET_VOLUME) return null;
        FluidStack fluidStack = getFluid();
        if (fluidStack != null && fluidStack.isFluidEqual(resource)) {
            if (doDrain) setFluid(null);
            return fluidStack;
        }
        return null;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (container.getCount() != 1 || maxDrain < Fluid.BUCKET_VOLUME) return null;
        FluidStack fluidStack = getFluid();
        if (fluidStack != null) {
            if (doDrain) setFluid(null);
            return fluidStack;
        }
        return null;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability != null && capability == CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return capability != null && capability == CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY ? (T) this : null;
    }
}
