package legacyconnector.v1_7_10.cofh.api.energy;

public interface IEnergyProvider extends IEnergyConnection {
    int extractEnergy(legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection from, int maxExtract, boolean simulate);

    int getEnergyStored(legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);

    int getMaxEnergyStored(legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);
}
