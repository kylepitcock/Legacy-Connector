package universalelectricity.api.energy;

import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

public interface IEnergyContainer {
    long getEnergy(ForgeDirection from);

    long getEnergyCapacity(ForgeDirection from);
}
