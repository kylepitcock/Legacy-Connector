package legacyconnector.v1_12_2.cofh.redstoneflux.util;

import legacyconnector.v1_12_2.cofh.redstoneflux.api.IEnergyContainerItem;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability;
import legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.ICapabilityProvider;
import legacyconnector.v1_12_2.net.minecraftforge.energy.CapabilityEnergy;
import legacyconnector.v1_12_2.net.minecraftforge.energy.IEnergyStorage;

/** Redstone Flux 1.12: the Forge Energy view of an RF container item stack (CoFH items return it from {@code initCapabilities}). */
public class EnergyContainerItemWrapper implements ICapabilityProvider {
    final ItemStack stack;
    final IEnergyContainerItem container;
    final boolean canExtract;
    final boolean canReceive;
    final IEnergyStorage energyCap;

    public EnergyContainerItemWrapper(ItemStack stack, IEnergyContainerItem container, boolean canExtract, boolean canReceive) {
        this.stack = stack;
        this.container = container;
        this.canExtract = canExtract;
        this.canReceive = canReceive;
        this.energyCap = new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return EnergyContainerItemWrapper.this.canReceive ? container.receiveEnergy(stack, maxReceive, simulate) : 0;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return EnergyContainerItemWrapper.this.canExtract ? container.extractEnergy(stack, maxExtract, simulate) : 0;
            }

            @Override
            public int getEnergyStored() {
                return container.getEnergyStored(stack);
            }

            @Override
            public int getMaxEnergyStored() {
                return container.getMaxEnergyStored(stack);
            }

            @Override
            public boolean canExtract() {
                return EnergyContainerItemWrapper.this.canExtract;
            }

            @Override
            public boolean canReceive() {
                return EnergyContainerItemWrapper.this.canReceive;
            }
        };
    }

    public EnergyContainerItemWrapper(ItemStack stack, IEnergyContainerItem container) {
        this(stack, container, true, true);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY ? (T) energyCap : null;
    }
}
