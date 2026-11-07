package legacyconnector.v1_12_2.cofh.redstoneflux.api;

import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

public interface IEnergyReceiver extends IEnergyHandler {
    int receiveEnergy(EnumFacing from, int maxReceive, boolean simulate);
}
