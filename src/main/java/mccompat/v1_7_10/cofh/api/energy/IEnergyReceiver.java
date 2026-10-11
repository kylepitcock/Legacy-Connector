package mccompat.v1_7_10.cofh.api.energy;

public interface IEnergyReceiver extends IEnergyConnection {
    int receiveEnergy(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from, int maxReceive, boolean simulate);

    int getEnergyStored(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);

    int getMaxEnergyStored(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);
}
