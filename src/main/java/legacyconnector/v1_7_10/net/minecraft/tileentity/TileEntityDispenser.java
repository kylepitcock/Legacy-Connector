package legacyconnector.v1_7_10.net.minecraft.tileentity;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;

/**
 * The dispenser's (and dropper's) nine slots. A view of a vanilla dispenser reads and writes the modern block entity; dispenser
 * behaviours use {@link #func_146019_a} to put what they produced (a filled bucket) back.
 */
public class TileEntityDispenser extends TileEntity implements IInventory {
    private ItemStack[] field_146022_i = new ItemStack[9];
    private final Random field_146021_j = new Random();
    protected String field_146020_a;
    private final net.minecraft.world.level.block.entity.DispenserBlockEntity mcc$dispenser;

    public TileEntityDispenser() {
        this.mcc$dispenser = null;
    }

    /** View of a vanilla dispenser or dropper. */
    public TileEntityDispenser(net.minecraft.world.level.block.entity.DispenserBlockEntity dispenser) {
        this.mcc$dispenser = dispenser;
    }

    @Override
    public int getSizeInventory() {
        return mcc$dispenser != null ? mcc$dispenser.getContainerSize() : field_146022_i.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (mcc$dispenser != null) return ItemStack.mcc$wrap(mcc$dispenser.getItem(slot));
        return slot >= 0 && slot < field_146022_i.length ? field_146022_i[slot] : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (mcc$dispenser != null) return ItemStack.mcc$wrap(mcc$dispenser.removeItem(slot, amount));
        ItemStack stack = field_146022_i[slot];
        if (stack == null) return null;
        if (stack.mcc$count() <= amount) {
            field_146022_i[slot] = null;
            markDirty();
            return stack;
        }
        ItemStack split = stack.splitStack(amount);
        if (stack.mcc$count() == 0) field_146022_i[slot] = null;
        markDirty();
        return split;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (mcc$dispenser != null) return ItemStack.mcc$wrap(mcc$dispenser.removeItemNoUpdate(slot));
        ItemStack stack = field_146022_i[slot];
        field_146022_i[slot] = null;
        return stack;
    }

    /** {@code func_146017_i}: a random slot that holds something, or -1. */
    public int func_146017_i() {
        int chosen = -1;
        int seen = 1;
        for (int i = 0; i < getSizeInventory(); ++i) {
            if (getStackInSlot(i) != null && field_146021_j.nextInt(seen++) == 0) chosen = i;
        }
        return chosen;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (mcc$dispenser != null) {
            mcc$dispenser.setItem(slot, ItemStack.mcc$toModern(stack));
            return;
        }
        field_146022_i[slot] = stack;
        if (stack != null && stack.mcc$count() > getInventoryStackLimit()) stack.mcc$setCount(getInventoryStackLimit());
        markDirty();
    }

    /** {@code func_146019_a}: puts the stack into the first empty slot; that slot, or -1 when none is free. */
    public int func_146019_a(ItemStack stack) {
        for (int i = 0; i < getSizeInventory(); ++i) {
            ItemStack existing = getStackInSlot(i);
            if (existing == null || existing.getItem() == null) {
                setInventorySlotContents(i, stack);
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getInventoryName() {
        return hasCustomInventoryName() ? field_146020_a : "container.dispenser";
    }

    public void func_146018_a(String name) {
        this.field_146020_a = name;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return field_146020_a != null;
    }

    @Override
    public int getInventoryStackLimit() {
        return mcc$dispenser != null ? mcc$dispenser.getMaxStackSize() : 64;
    }

    @Override
    public void markDirty() {
        if (mcc$dispenser != null) mcc$dispenser.setChanged();
        else super.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (mcc$dispenser != null) return mcc$dispenser.stillValid(player.mcc$player());
        return worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this && player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        NBTTagList items = tag.getTagList("Items", 10);
        field_146022_i = new ItemStack[9];
        for (int i = 0; i < items.tagCount(); ++i) {
            NBTTagCompound item = items.getCompoundTagAt(i);
            int slot = item.getByte("Slot") & 255;
            if (slot < field_146022_i.length) field_146022_i[slot] = ItemStack.loadItemStackFromNBT(item);
        }
        if (tag.hasKey("CustomName", 8)) field_146020_a = tag.getString("CustomName");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        NBTTagList items = new NBTTagList();
        for (int i = 0; i < field_146022_i.length; ++i) {
            if (field_146022_i[i] != null) {
                NBTTagCompound item = new NBTTagCompound();
                item.setByte("Slot", (byte) i);
                field_146022_i[i].writeToNBT(item);
                items.appendTag(item);
            }
        }
        tag.setTag("Items", items);
        if (hasCustomInventoryName()) tag.setString("CustomName", field_146020_a);
    }
}
