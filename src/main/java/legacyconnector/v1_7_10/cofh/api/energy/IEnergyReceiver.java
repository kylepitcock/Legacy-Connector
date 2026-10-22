package legacyconnector.v1_7_10.cofh.api.energy;

public interface IEnergyReceiver extends IEnergyConnection {
    int receiveEnergy(legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection from, int maxReceive, boolean simulate);

    int getEnergyStored(legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);

    int getMaxEnergyStored(legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);
}
