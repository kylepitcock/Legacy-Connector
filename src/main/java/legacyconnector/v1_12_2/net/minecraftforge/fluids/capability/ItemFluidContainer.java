package legacyconnector.v1_12_2.net.minecraftforge.fluids.capability;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider;

/** Forge: an item that holds one fluid up to a capacity (in mB), kept in the stack's NBT by a FluidHandlerItemStack. */
public class ItemFluidContainer extends Item {
    protected final int capacity;

    public ItemFluidContainer(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
        return new legacyconnector.v1_12_2.net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack(stack, capacity);
    }
}
