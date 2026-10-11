package mccompat.v1_7_10.cofh.api.energy;

public interface IEnergyHandler extends IEnergyProvider, IEnergyReceiver {
    @Override
    int receiveEnergy(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from, int maxReceive, boolean simulate);

    @Override
    int extractEnergy(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from, int maxExtract, boolean simulate);

    @Override
    int getEnergyStored(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);

    @Override
    int getMaxEnergyStored(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);
}
