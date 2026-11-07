package buildcraft.api.power;

import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/** Test double of BuildCraft 3.x's provider. */
public interface IPowerProvider {
    int getMinEnergyReceived();

    int getMaxEnergyReceived();

    float getEnergyStored();

    int getMaxEnergyStored();

    void receiveEnergy(float quantity, ForgeDirection from);
}
