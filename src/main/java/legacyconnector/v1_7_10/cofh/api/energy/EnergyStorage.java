package legacyconnector.v1_7_10.cofh.api.energy;

import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/** Reference RF buffer; NBT key "Energy" as in CoFH's API. */
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
        this.energy = Math.min(nbt.getInteger("Energy"), capacity);
        if (energy < 0) energy = 0;
        return this;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("Energy", Math.max(0, energy));
        return nbt;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        if (energy > capacity) energy = capacity;
    }

    public void setMaxTransfer(int maxTransfer) {
        setMaxReceive(maxTransfer);
        setMaxExtract(maxTransfer);
    }

    public void setMaxReceive(int maxReceive) {
        this.maxReceive = maxReceive;
    }

    public void setMaxExtract(int maxExtract) {
        this.maxExtract = maxExtract;
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
        // never negative: Traincraft's windmill adds round(y * 0.25) * 10 to the wind, which is below zero under y = 0 (modern worlds
        // reach y = -64; superflat ground is at -60), and a negative "receive" drained the buffer to -4000 FE
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
