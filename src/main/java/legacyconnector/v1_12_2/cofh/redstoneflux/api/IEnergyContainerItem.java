package legacyconnector.v1_12_2.cofh.redstoneflux.api;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

public interface IEnergyContainerItem {
    int receiveEnergy(ItemStack container, int maxReceive, boolean simulate);

    int extractEnergy(ItemStack container, int maxExtract, boolean simulate);

    int getEnergyStored(ItemStack container);

    int getMaxEnergyStored(ItemStack container);
}
