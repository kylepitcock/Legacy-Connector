package legacyconnector.v1_7_10.cofh.api.energy;

public interface IEnergyHandler extends IEnergyProvider, IEnergyReceiver {
    @Override
    int receiveEnergy(legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection from, int maxReceive, boolean simulate);

    @Override
    int extractEnergy(legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection from, int maxExtract, boolean simulate);

    @Override
    int getEnergyStored(legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);

    @Override
    int getMaxEnergyStored(legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);
}
