package legacyconnector.v1_7_10.cofh.api.energy;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

public interface IEnergyContainerItem {
    int receiveEnergy(ItemStack container, int maxReceive, boolean simulate);

    int extractEnergy(ItemStack container, int maxExtract, boolean simulate);

    int getEnergyStored(ItemStack container);

    int getMaxEnergyStored(ItemStack container);
}
