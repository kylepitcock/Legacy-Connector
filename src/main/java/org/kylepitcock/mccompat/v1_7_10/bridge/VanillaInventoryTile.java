package org.kylepitcock.mccompat.v1_7_10.bridge;

import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.inventory.IInventory;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.tileentity.TileEntity;
import net.minecraft.world.Container;

/** Legacy view of any modern block entity that is a {@link Container} (furnaces, hoppers, barrels, modern mods). */
public class VanillaInventoryTile extends TileEntity implements IInventory {
    private final Container inv;

    public VanillaInventoryTile(Container inv) {
        this.inv = inv;
    }

    @Override
    public int getSizeInventory() {
        return inv.getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.mcc$wrap(inv.getItem(slot));
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        return ItemStack.mcc$wrap(inv.removeItem(slot, amount));
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return ItemStack.mcc$wrap(inv.removeItemNoUpdate(slot));
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        inv.setItem(slot, ItemStack.mcc$toModern(stack));
    }

    @Override
    public String getInventoryName() {
        return "container.inventory";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return inv.getMaxStackSize();
    }

    @Override
    public void markDirty() {
        inv.setChanged();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return inv.stillValid(player.mcc$player());
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return inv.canPlaceItem(slot, ItemStack.mcc$toModern(stack));
    }
}
