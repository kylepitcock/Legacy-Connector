package org.kylepitcock.legacyconnector.v1_12_2.gui;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.ClickType;
import legacyconnector.v1_12_2.net.minecraft.inventory.ContainerPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * The player's modern inventory menu as 1.12.2's {@code player.inventoryContainer}: a {@link ContainerPlayer} (legacy code casts
 * {@code GuiInventory.inventorySlots} to one) whose slots are views of the menu's slots, in 1.12's order (the modern inventory menu
 * numbers them the same: result, 2x2 grid, armour, inventory, hotbar, offhand). Clicks and crafting are the modern menu's.
 */
public class ModernPlayerContainer1122 extends ContainerPlayer implements ModernMenuView1122 {
    private final AbstractContainerMenu modern;

    ModernPlayerContainer1122(AbstractContainerMenu modern) {
        super(null, true);
        this.modern = modern;
        this.windowId = modern.containerId;
        mcc$sync();
    }

    @Override
    public AbstractContainerMenu mcc$modern() {
        return modern;
    }

    @Override
    public void mcc$sync() {
        while (inventorySlots.size() < modern.slots.size()) {
            addSlotToContainer(new ModernContainer1122.ViewSlot(modern.slots.get(inventorySlots.size())));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return ModernMenuView1122.canInteract(modern, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return ModernMenuView1122.transfer(modern, player, index);
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        return ModernMenuView1122.click(this, modern, slotId, dragType, clickType, player);
    }

    /** The modern menu crafts: the legacy grid of this view is never filled. */
    @Override
    public void onCraftMatrixChanged(IInventory inventoryIn) {
    }

    @Override
    public void detectAndSendChanges() {
        modern.broadcastChanges();
        if (!listeners.isEmpty()) {
            mcc$sync();
            super.detectAndSendChanges();
        }
    }

    @Override
    public void mcc$tickListeners() {
        if (!listeners.isEmpty()) detectAndSendChanges();
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
    }
}
