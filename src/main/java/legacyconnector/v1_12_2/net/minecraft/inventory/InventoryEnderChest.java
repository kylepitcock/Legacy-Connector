package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * 1.12.2 ender chest inventory. {@code player.getInventoryEnderChest()} is a live view of the player's modern ender chest; one a mod
 * constructs itself is a plain 27-slot inventory.
 */
public class InventoryEnderChest extends InventoryBasic {
    private final net.minecraft.world.inventory.PlayerEnderChestContainer mcc$modern;

    public InventoryEnderChest() {
        this(null);
    }

    public InventoryEnderChest(net.minecraft.world.inventory.PlayerEnderChestContainer modern) {
        super("container.enderchest", false, 27);
        this.mcc$modern = modern;
    }

    public net.minecraft.world.inventory.PlayerEnderChestContainer mcc$modern() {
        return mcc$modern;
    }

    public void loadInventoryFromNBT(NBTTagList list) {
        for (int i = 0; i < getSizeInventory(); ++i) setInventorySlotContents(i, ItemStack.EMPTY);
        for (int i = 0; i < list.tagCount(); ++i) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getByte("Slot") & 255;
            if (slot < getSizeInventory()) setInventorySlotContents(slot, new ItemStack(tag));
        }
    }

    public NBTTagList saveInventoryToNBT() {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < getSizeInventory(); ++i) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setByte("Slot", (byte) i);
                stack.writeToNBT(tag);
                list.appendTag(tag);
            }
        }
        return list;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (mcc$modern == null) return super.getStackInSlot(index);
        return index >= 0 && index < mcc$modern.getContainerSize() ? Stacks1122.view(mcc$modern.getItem(index)) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return mcc$modern == null ? super.decrStackSize(index, count) : Stacks1122.fromModern(mcc$modern.removeItem(index, count));
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return mcc$modern == null ? super.removeStackFromSlot(index) : Stacks1122.fromModern(mcc$modern.removeItemNoUpdate(index));
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (mcc$modern == null) super.setInventorySlotContents(index, stack);
        else mcc$modern.setItem(index, Stacks1122.toModernLive(stack));
    }

    @Override
    public boolean isEmpty() {
        return mcc$modern == null ? super.isEmpty() : mcc$modern.isEmpty();
    }

    @Override
    public void markDirty() {
        if (mcc$modern == null) {
            super.markDirty();
            return;
        }
        Stacks1122.flushViews();
        mcc$modern.setChanged();
    }

    /** The ender chest block the inventory is open at (its lid opens and closes with it), or null. */
    private legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityEnderChest associatedChest;

    public void setChestTileEntity(legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntityEnderChest chestTileEntity) {
        this.associatedChest = chestTileEntity;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        if (associatedChest != null && !associatedChest.canBeUsed(player)) return false;
        if (mcc$modern == null || player == null || !(player.mcc$entity instanceof net.minecraft.world.entity.player.Player p)) return true;
        return mcc$modern.stillValid(p);
    }

    @Override
    public void openInventory(EntityPlayer player) {
        if (associatedChest != null) associatedChest.openChest();
        if (mcc$modern != null && player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) mcc$modern.startOpen(p);
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        if (associatedChest != null) associatedChest.closeChest();
        associatedChest = null;
        if (mcc$modern != null && player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) mcc$modern.stopOpen(p);
        super.closeInventory(player);
    }

    @Override
    public void clear() {
        if (mcc$modern == null) super.clear();
        else mcc$modern.clearContent();
    }
}
