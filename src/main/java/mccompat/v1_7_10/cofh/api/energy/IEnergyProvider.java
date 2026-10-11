package mccompat.v1_7_10.cofh.api.energy;

public interface IEnergyProvider extends IEnergyConnection {
    int extractEnergy(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from, int maxExtract, boolean simulate);

    int getEnergyStored(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);

    int getMaxEnergyStored(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from);
}
