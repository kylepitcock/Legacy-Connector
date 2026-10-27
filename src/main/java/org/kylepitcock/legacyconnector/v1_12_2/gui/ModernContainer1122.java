package org.kylepitcock.legacyconnector.v1_12_2.gui;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.ClickType;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.Slot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * A modern menu (the player's inventory, a chest, a villager's trades) as a 1.12.2 {@link Container}: the same slots in the same order and
 * GUI positions, each a {@link ViewSlot} over the modern slot. Legacy GUIs built over the player's inventory ({@code GuiInventory}) and the
 * legacy stand-ins of the game's container screens use it; clicks go through the player's modern menu, as 1.12's went through
 * {@code player.openContainer}.
 */
public class ModernContainer1122 extends Container {
    private static final Map<AbstractContainerMenu, ModernContainer1122> VIEWS = new WeakHashMap<>();
    private final AbstractContainerMenu modern;

    protected ModernContainer1122(AbstractContainerMenu modern) {
        this.modern = modern;
        this.windowId = modern.containerId;
        mcc$sync();
    }

    /** The view of a modern menu (one per menu). */
    public static ModernContainer1122 of(AbstractContainerMenu modern) {
        if (modern == null) return null;
        ModernContainer1122 view;
        synchronized (VIEWS) {
            view = VIEWS.computeIfAbsent(modern, ModernContainer1122::new);
        }
        view.mcc$sync();
        return view;
    }

    public AbstractContainerMenu mcc$modern() {
        return modern;
    }

    /** Adds views for modern slots added since the last call (menus add slots in their constructors, a few later). */
    public void mcc$sync() {
        while (inventorySlots.size() < modern.slots.size()) addSlotToContainer(new ViewSlot(modern.slots.get(inventorySlots.size())));
    }

    private static net.minecraft.world.entity.player.Player modern(EntityPlayer player) {
        return player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p ? p : null;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        var p = modern(player);
        return p == null || modern.stillValid(p);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        var p = modern(player);
        return p == null ? ItemStack.EMPTY : Stacks1122.fromModern(modern.quickMoveStack(p, index));
    }

    /** A click as the player makes it: on the client through the game mode (the server is told), elsewhere on the menu directly. */
    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        ItemStack before = slotId >= 0 && slotId < inventorySlots.size() ? inventorySlots.get(slotId).getStack().copy() : ItemStack.EMPTY;
        var p = modern(player);
        if (p == null) return before;
        var type = clickType == null ? net.minecraft.world.inventory.ClickType.PICKUP : clickType.mcc$modern();
        if (p.level().isClientSide) {
            org.kylepitcock.legacyconnector.v1_12_2.client.ClientMenu1122.click(modern.containerId, slotId, dragType, type);
        } else {
            modern.clicked(slotId, dragType, type, p);
        }
        return before;
    }

    @Override
    public void detectAndSendChanges() {
        modern.broadcastChanges();
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
    }

    /** A modern slot as a 1.12.2 slot: its stack (a live view), validity, limits and the background sprite of empty armour slots. */
    public static class ViewSlot extends Slot {
        private final net.minecraft.world.inventory.Slot modern;

        public ViewSlot(net.minecraft.world.inventory.Slot modern) {
            super(ModernInventory1122.of(modern.container), modern.getContainerSlot(), modern.x, modern.y);
            this.modern = modern;
            var icon = modern.getNoItemIcon();
            if (icon != null) setBackgroundName(icon.getSecond().toString());
        }

        public net.minecraft.world.inventory.Slot mcc$modern() {
            return modern;
        }

        @Override
        public ItemStack getStack() {
            return Stacks1122.view(modern.getItem());
        }

        @Override
        public void putStack(ItemStack stack) {
            modern.set(Stacks1122.toModernLive(stack));
        }

        @Override
        public void onSlotChanged() {
            modern.setChanged();
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return modern.mayPlace(Stacks1122.toModern(stack));
        }

        @Override
        public int getSlotStackLimit() {
            return modern.getMaxStackSize();
        }

        @Override
        public int getItemStackLimit(ItemStack stack) {
            return modern.getMaxStackSize(Stacks1122.toModern(stack));
        }

        @Override
        public ItemStack decrStackSize(int amount) {
            return Stacks1122.fromModern(modern.remove(amount));
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            var p = modern(player);
            return p == null || modern.mayPickup(p);
        }

        @Override
        public boolean isEnabled() {
            return modern.isActive();
        }

        @Override
        public ItemStack onTake(EntityPlayer player, ItemStack stack) {
            var p = modern(player);
            if (p != null) modern.onTake(p, Stacks1122.toModernLive(stack));
            return stack;
        }
    }
}
