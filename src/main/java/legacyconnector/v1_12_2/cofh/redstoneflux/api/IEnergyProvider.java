package legacyconnector.v1_12_2.cofh.redstoneflux.api;

import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

public interface IEnergyProvider extends IEnergyHandler {
    int extractEnergy(EnumFacing from, int maxExtract, boolean simulate);
}
