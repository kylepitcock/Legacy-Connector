package legacyconnector.v1_7_10.baubles.common.container;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Baubles' internal inventory class, which mods such as Botania reach through PlayerHandler; backed by the Curios bridge. */
public class InventoryBaubles implements IInventory {
    public legacyconnector.v1_7_10.net.minecraft.inventory.Container eventHandler;
    public EntityPlayer player;
    private final IInventory backing;

    public InventoryBaubles(EntityPlayer player) {
        this.player = player;
        this.backing = legacyconnector.v1_7_10.baubles.api.BaublesApi.getBaubles(player);
    }

    public void setEventHandler(legacyconnector.v1_7_10.net.minecraft.inventory.Container handler) {
        this.eventHandler = handler;
    }

    public void syncSlotToClients(int slot) {
    }

    public void dropItems(java.util.ArrayList<?> drops) {
    }

    @Override
    public int getSizeInventory() {
        return backing == null ? 4 : backing.getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return backing == null ? null : backing.getStackInSlot(slot);
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        return backing == null ? null : backing.decrStackSize(slot, amount);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return backing == null ? null : backing.getStackInSlotOnClosing(slot);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (backing != null) backing.setInventorySlotContents(slot, stack);
    }

    @Override
    public String getInventoryName() {
        return "baubles";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer p) {
        return true;
    }

    @Override
    public void openInventory() {
    }

    @Override
    public void closeInventory() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return backing == null || backing.isItemValidForSlot(slot, stack);
    }
}
