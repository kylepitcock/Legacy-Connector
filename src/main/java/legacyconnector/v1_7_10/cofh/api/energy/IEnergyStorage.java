package legacyconnector.v1_7_10.cofh.api.energy;

public interface IEnergyStorage {
    int receiveEnergy(int maxReceive, boolean simulate);

    int extractEnergy(int maxExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();
}
