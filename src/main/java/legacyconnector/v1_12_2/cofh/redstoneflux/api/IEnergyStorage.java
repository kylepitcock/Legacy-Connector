package legacyconnector.v1_12_2.cofh.redstoneflux.api;

public interface IEnergyStorage {
    int receiveEnergy(int maxReceive, boolean simulate);

    int extractEnergy(int maxExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();
}
