package legacyconnector.v1_12_2.cofh.redstoneflux.impl;

import legacyconnector.v1_12_2.cofh.redstoneflux.api.IEnergyProvider;
import legacyconnector.v1_12_2.cofh.redstoneflux.api.IEnergyReceiver;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** Reference RF tile entity of Redstone Flux 1.12 with one internal buffer. */
public class TileEnergyHandler extends TileEntity implements IEnergyReceiver, IEnergyProvider {
    protected EnergyStorage storage = new EnergyStorage(32000);

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        storage.readFromNBT(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        storage.writeToNBT(nbt);
        return nbt;
    }

    @Override
    public boolean canConnectEnergy(EnumFacing from) {
        return true;
    }

    @Override
    public int receiveEnergy(EnumFacing from, int maxReceive, boolean simulate) {
        return storage.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(EnumFacing from, int maxExtract, boolean simulate) {
        return storage.extractEnergy(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored(EnumFacing from) {
        return storage.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored(EnumFacing from) {
        return storage.getMaxEnergyStored();
    }
}
