package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;
import net.minecraft.world.inventory.PlayerEnderChestContainer;

/** A player's ender chest (27 slots): a view of the modern player's ender chest inventory, so it is shared with vanilla ender chests. */
public class InventoryEnderChest extends InventoryBasic {
    private final PlayerEnderChestContainer mcc$modern;

    public InventoryEnderChest() {
        this(null);
    }

    /** View of a modern player's ender chest. */
    public InventoryEnderChest(PlayerEnderChestContainer modern) {
        super("container.enderchest", false, 27);
        this.mcc$modern = modern;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (mcc$modern == null) return super.getStackInSlot(slot);
        return slot >= 0 && slot < mcc$modern.getContainerSize() ? ItemStack.mcc$wrap(mcc$modern.getItem(slot)) : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (mcc$modern == null) return super.decrStackSize(slot, amount);
        var taken = mcc$modern.removeItem(slot, amount);
        return taken.isEmpty() ? null : ItemStack.mcc$wrap(taken);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (mcc$modern == null) return super.getStackInSlotOnClosing(slot);
        var taken = mcc$modern.removeItemNoUpdate(slot);
        return taken.isEmpty() ? null : ItemStack.mcc$wrap(taken);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (mcc$modern == null) {
            super.setInventorySlotContents(slot, stack);
            return;
        }
        mcc$modern.setItem(slot, stack == null ? net.minecraft.world.item.ItemStack.EMPTY : stack.mcc$modern());
    }

    @Override
    public void markDirty() {
        if (mcc$modern != null) mcc$modern.setChanged();
        super.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return mcc$modern == null || player == null || mcc$modern.stillValid(player.mcc$player());
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    public void loadInventoryFromNBT(NBTTagList list) {
        for (int i = 0; i < getSizeInventory(); i++) setInventorySlotContents(i, null);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getByte("Slot") & 255;
            if (slot < getSizeInventory()) setInventorySlotContents(slot, ItemStack.loadItemStackFromNBT(tag));
        }
    }

    public NBTTagList saveInventoryToNBT() {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < getSizeInventory(); i++) {
            ItemStack stack = getStackInSlot(i);
            if (stack == null) continue;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setByte("Slot", (byte) i);
            stack.writeToNBT(tag);
            list.appendTag(tag);
        }
        return list;
    }
}
