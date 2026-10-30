package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.InventoryPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation;
import legacyconnector.v1_12_2.net.minecraft.world.ILockableContainer;
import legacyconnector.v1_12_2.net.minecraft.world.LockCode;

/** 1.12.2's double chest: two chest inventories as one (the upper one's slots first). */
public class InventoryLargeChest implements ILockableContainer {
    private final String name;
    private final ILockableContainer upperChest;
    private final ILockableContainer lowerChest;

    public InventoryLargeChest(String nameIn, ILockableContainer upperChestIn, ILockableContainer lowerChestIn) {
        this.name = nameIn;
        if (upperChestIn == null) upperChestIn = lowerChestIn;
        if (lowerChestIn == null) lowerChestIn = upperChestIn;
        this.upperChest = upperChestIn;
        this.lowerChest = lowerChestIn;
        if (upperChestIn.isLocked()) lowerChestIn.setLockCode(upperChestIn.getLockCode());
        else if (lowerChestIn.isLocked()) upperChestIn.setLockCode(lowerChestIn.getLockCode());
    }

    @Override
    public int getSizeInventory() {
        return upperChest.getSizeInventory() + lowerChest.getSizeInventory();
    }

    @Override
    public boolean isEmpty() {
        return upperChest.isEmpty() && lowerChest.isEmpty();
    }

    /** Whether the inventory is one of the two halves. */
    public boolean isPartOfLargeChest(IInventory inventoryIn) {
        return upperChest == inventoryIn || lowerChest == inventoryIn;
    }

    @Override
    public String getName() {
        if (upperChest.hasCustomName()) return upperChest.getName();
        return lowerChest.hasCustomName() ? lowerChest.getName() : name;
    }

    @Override
    public boolean hasCustomName() {
        return upperChest.hasCustomName() || lowerChest.hasCustomName();
    }

    @Override
    public ITextComponent getDisplayName() {
        return hasCustomName() ? new TextComponentString(getName()) : new TextComponentTranslation(getName());
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        int upper = upperChest.getSizeInventory();
        return index >= upper ? lowerChest.getStackInSlot(index - upper) : upperChest.getStackInSlot(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        int upper = upperChest.getSizeInventory();
        return index >= upper ? lowerChest.decrStackSize(index - upper, count) : upperChest.decrStackSize(index, count);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        int upper = upperChest.getSizeInventory();
        return index >= upper ? lowerChest.removeStackFromSlot(index - upper) : upperChest.removeStackFromSlot(index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        int upper = upperChest.getSizeInventory();
        if (index >= upper) lowerChest.setInventorySlotContents(index - upper, stack);
        else upperChest.setInventorySlotContents(index, stack);
    }

    @Override
    public int getInventoryStackLimit() {
        return upperChest.getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        upperChest.markDirty();
        lowerChest.markDirty();
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return upperChest.isUsableByPlayer(player) && lowerChest.isUsableByPlayer(player);
    }

    @Override
    public void openInventory(EntityPlayer player) {
        upperChest.openInventory(player);
        lowerChest.openInventory(player);
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        upperChest.closeInventory(player);
        lowerChest.closeInventory(player);
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public boolean isLocked() {
        return upperChest.isLocked() || lowerChest.isLocked();
    }

    @Override
    public void setLockCode(LockCode code) {
        upperChest.setLockCode(code);
        lowerChest.setLockCode(code);
    }

    @Override
    public LockCode getLockCode() {
        return upperChest.getLockCode();
    }

    @Override
    public String getGuiID() {
        return upperChest.getGuiID();
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        return new ContainerChest(playerInventory, this, playerIn);
    }

    @Override
    public void clear() {
        upperChest.clear();
        lowerChest.clear();
    }
}
