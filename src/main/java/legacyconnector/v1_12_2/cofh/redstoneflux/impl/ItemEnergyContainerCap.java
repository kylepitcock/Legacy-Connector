package legacyconnector.v1_12_2.cofh.redstoneflux.impl;

import legacyconnector.v1_12_2.cofh.redstoneflux.util.EnergyContainerItemWrapper;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider;

/** An RF container item that also answers Forge Energy (the wrapper of Redstone Flux 1.12 turns the item's RF methods into the capability). */
public class ItemEnergyContainerCap extends ItemEnergyContainer {
    public ItemEnergyContainerCap() {
        super();
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
        return new EnergyContainerItemWrapper(stack, this);
    }
}
