package legacyconnector.v1_7_10.net.minecraftforge.fluids;

import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/** Forge 1.7.10 base for items that store a fluid in their NBT ("Fluid"). */
public class ItemFluidContainer extends Item implements IFluidContainerItem {
    protected int capacity;

    public ItemFluidContainer() {
        this(FluidContainerRegistry.BUCKET_VOLUME);
    }

    public ItemFluidContainer(int capacity) {
        super();
        this.capacity = capacity;
        this.setMaxStackSize(1);
    }

    public ItemFluidContainer setCapacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    @Override
    public FluidStack getFluid(ItemStack container) {
        if (ItemStack.mcc$get$stackTagCompound(container) == null || !ItemStack.mcc$get$stackTagCompound(container).hasKey("Fluid")) return null;
        return FluidStack.loadFluidStackFromNBT(ItemStack.mcc$get$stackTagCompound(container).getCompoundTag("Fluid"));
    }

    @Override
    public int getCapacity(ItemStack container) {
        return capacity;
    }

    @Override
    public int fill(ItemStack container, FluidStack resource, boolean doFill) {
        if (resource == null) return 0;
        if (!doFill) {
            if (ItemStack.mcc$get$stackTagCompound(container) == null || !ItemStack.mcc$get$stackTagCompound(container).hasKey("Fluid")) return Math.min(capacity, resource.amount);
            FluidStack stack = FluidStack.loadFluidStackFromNBT(ItemStack.mcc$get$stackTagCompound(container).getCompoundTag("Fluid"));
            if (stack == null) return Math.min(capacity, resource.amount);
            if (!stack.isFluidEqual(resource)) return 0;
            return Math.min(capacity - stack.amount, resource.amount);
        }
        if (ItemStack.mcc$get$stackTagCompound(container) == null) ItemStack.mcc$set$stackTagCompound(container, new NBTTagCompound());
        if (!ItemStack.mcc$get$stackTagCompound(container).hasKey("Fluid")) {
            NBTTagCompound fluidTag = resource.writeToNBT(new NBTTagCompound());
            if (capacity < resource.amount) {
                fluidTag.setInteger("Amount", capacity);
                ItemStack.mcc$get$stackTagCompound(container).setTag("Fluid", fluidTag);
                return capacity;
            }
            ItemStack.mcc$get$stackTagCompound(container).setTag("Fluid", fluidTag);
            return resource.amount;
        }
        NBTTagCompound fluidTag = ItemStack.mcc$get$stackTagCompound(container).getCompoundTag("Fluid");
        FluidStack stack = FluidStack.loadFluidStackFromNBT(fluidTag);
        if (!stack.isFluidEqual(resource)) return 0;
        int filled = capacity - stack.amount;
        if (resource.amount < filled) {
            stack.amount += resource.amount;
            filled = resource.amount;
        } else {
            stack.amount = capacity;
        }
        ItemStack.mcc$get$stackTagCompound(container).setTag("Fluid", stack.writeToNBT(fluidTag));
        return filled;
    }

    @Override
    public FluidStack drain(ItemStack container, int maxDrain, boolean doDrain) {
        if (ItemStack.mcc$get$stackTagCompound(container) == null || !ItemStack.mcc$get$stackTagCompound(container).hasKey("Fluid")) return null;
        FluidStack stack = FluidStack.loadFluidStackFromNBT(ItemStack.mcc$get$stackTagCompound(container).getCompoundTag("Fluid"));
        if (stack == null) return null;
        int drained = Math.min(stack.amount, maxDrain);
        if (doDrain) {
            if (maxDrain >= stack.amount) {
                ItemStack.mcc$get$stackTagCompound(container).removeTag("Fluid");
                if (ItemStack.mcc$get$stackTagCompound(container).hasNoTags()) ItemStack.mcc$set$stackTagCompound(container, null);
                return stack;
            }
            NBTTagCompound fluidTag = ItemStack.mcc$get$stackTagCompound(container).getCompoundTag("Fluid");
            fluidTag.setInteger("Amount", fluidTag.getInteger("Amount") - drained);
            ItemStack.mcc$get$stackTagCompound(container).setTag("Fluid", fluidTag);
        }
        stack.amount = drained;
        return stack;
    }
}
