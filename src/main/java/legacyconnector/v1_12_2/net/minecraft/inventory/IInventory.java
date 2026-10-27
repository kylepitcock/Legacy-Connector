package legacyconnector.v1_12_2.net.minecraft.inventory;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

public interface IInventory extends legacyconnector.v1_12_2.net.minecraft.world.IWorldNameable {
    int getSizeInventory();

    boolean isEmpty();

    ItemStack getStackInSlot(int index);

    ItemStack decrStackSize(int index, int count);

    ItemStack removeStackFromSlot(int index);

    void setInventorySlotContents(int index, ItemStack stack);

    int getInventoryStackLimit();

    void markDirty();

    boolean isUsableByPlayer(EntityPlayer player);

    void openInventory(EntityPlayer player);

    void closeInventory(EntityPlayer player);

    boolean isItemValidForSlot(int index, ItemStack stack);

    int getField(int id);

    void setField(int id, int value);

    int getFieldCount();

    void clear();

    String getName();

    boolean hasCustomName();

    /** IWorldNameable: the name as a text component (the custom name as is, else the name as a translation key). */
    @Override
    default legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getDisplayName() {
        return hasCustomName() ? new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString(getName())
                : new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation(getName());
    }
}
