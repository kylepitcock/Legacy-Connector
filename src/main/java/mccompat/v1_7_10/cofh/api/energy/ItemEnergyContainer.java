package mccompat.v1_7_10.cofh.api.energy;

import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.nbt.NBTTagCompound;

/** Reference RF item: energy in the stack's "Energy" tag. */
public class ItemEnergyContainer extends Item implements IEnergyContainerItem {
    protected int capacity;
    protected int maxReceive;
    protected int maxExtract;

    public ItemEnergyContainer() {
    }

    public ItemEnergyContainer(int capacity) {
        this(capacity, capacity, capacity);
    }

    public ItemEnergyContainer(int capacity, int maxTransfer) {
        this(capacity, maxTransfer, maxTransfer);
    }

    public ItemEnergyContainer(int capacity, int maxReceive, int maxExtract) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    public ItemEnergyContainer setCapacity(int capacity) {
        this.capacity = capacity;
        return this;
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

    private static NBTTagCompound tag(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound();
    }

    @Override
    public int receiveEnergy(ItemStack container, int maxReceive, boolean simulate) {
        NBTTagCompound tag = tag(container);
        int energy = tag.getInteger("Energy");
        int received = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
        if (!simulate) tag.setInteger("Energy", energy + received);
        return received;
    }

    @Override
    public int extractEnergy(ItemStack container, int maxExtract, boolean simulate) {
        if (!container.hasTagCompound() || !container.getTagCompound().hasKey("Energy")) return 0;
        NBTTagCompound tag = container.getTagCompound();
        int energy = tag.getInteger("Energy");
        int extracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
        if (!simulate) tag.setInteger("Energy", energy - extracted);
        return extracted;
    }

    @Override
    public int getEnergyStored(ItemStack container) {
        return container.hasTagCompound() ? container.getTagCompound().getInteger("Energy") : 0;
    }

    @Override
    public int getMaxEnergyStored(ItemStack container) {
        return capacity;
    }
}
