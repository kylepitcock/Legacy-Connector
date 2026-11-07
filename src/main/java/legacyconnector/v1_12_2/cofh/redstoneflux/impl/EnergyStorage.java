package legacyconnector.v1_12_2.cofh.redstoneflux.impl;

import legacyconnector.v1_12_2.cofh.redstoneflux.api.IEnergyStorage;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** Reference RF buffer of Redstone Flux 1.12 (NBT key "Energy"). */
public class EnergyStorage implements IEnergyStorage {
    protected int energy;
    protected int capacity;
    protected int maxReceive;
    protected int maxExtract;

    public EnergyStorage(int capacity) {
        this(capacity, capacity, capacity);
    }

    public EnergyStorage(int capacity, int maxTransfer) {
        this(capacity, maxTransfer, maxTransfer);
    }

    public EnergyStorage(int capacity, int maxReceive, int maxExtract) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    public EnergyStorage readFromNBT(NBTTagCompound nbt) {
        this.energy = nbt.getInteger("Energy");
        if (energy > capacity) energy = capacity;
        return this;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        if (energy < 0) energy = 0;
        nbt.setInteger("Energy", energy);
        return nbt;
    }

    public EnergyStorage setCapacity(int capacity) {
        this.capacity = capacity;
        if (energy > capacity) energy = capacity;
        return this;
    }

    public EnergyStorage setMaxTransfer(int maxTransfer) {
        setMaxReceive(maxTransfer);
        setMaxExtract(maxTransfer);
        return this;
    }

    public EnergyStorage setMaxReceive(int maxReceive) {
        this.maxReceive = maxReceive;
        return this;
    }

    public EnergyStorage setMaxExtract(int maxExtract) {
        this.maxExtract = maxExtract;
        return this;
    }

    public int getMaxReceive() {
        return maxReceive;
    }

    public int getMaxExtract() {
        return maxExtract;
    }

    public void setEnergyStored(int energy) {
        this.energy = Math.max(0, Math.min(capacity, energy));
    }

    public void modifyEnergyStored(int delta) {
        setEnergyStored(energy + delta);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = Math.max(0, Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive)));
        if (!simulate) energy += received;
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = Math.max(0, Math.min(energy, Math.min(this.maxExtract, maxExtract)));
        if (!simulate) energy -= extracted;
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }
}
