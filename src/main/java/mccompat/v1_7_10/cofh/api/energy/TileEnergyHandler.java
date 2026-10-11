package mccompat.v1_7_10.cofh.api.energy;

import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import mccompat.v1_7_10.net.minecraft.tileentity.TileEntity;

/** Reference RF tile entity with one internal buffer. */
public class TileEnergyHandler extends TileEntity implements IEnergyHandler {
    protected EnergyStorage storage = new EnergyStorage(32000);

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        storage.readFromNBT(nbt);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        storage.writeToNBT(nbt);
    }

    @Override
    public boolean canConnectEnergy(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from) {
        return true;
    }

    @Override
    public int receiveEnergy(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from, int maxReceive, boolean simulate) {
        return storage.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from, int maxExtract, boolean simulate) {
        return storage.extractEnergy(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from) {
        return storage.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored(mccompat.v1_7_10.net.minecraftforge.common.util.ForgeDirection from) {
        return storage.getMaxEnergyStored();
    }
}
