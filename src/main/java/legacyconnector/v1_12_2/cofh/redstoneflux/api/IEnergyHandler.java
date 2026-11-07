package legacyconnector.v1_12_2.cofh.redstoneflux.api;

import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

public interface IEnergyHandler extends IEnergyConnection {
    int getEnergyStored(EnumFacing from);

    int getMaxEnergyStored(EnumFacing from);
}
