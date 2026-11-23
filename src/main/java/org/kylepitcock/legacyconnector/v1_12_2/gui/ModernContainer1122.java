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
public class ModernContainer1122 extends Container implements ModernMenuView1122 {
    private static final Map<AbstractContainerMenu, ModernMenuView1122> VIEWS = new WeakHashMap<>();
    private final AbstractContainerMenu modern;

    protected ModernContainer1122(AbstractContainerMenu modern) {
        this.modern = modern;
        this.windowId = modern.containerId;
        mcc$sync();
    }

    /**
     * The view of a modern menu (one per menu). The player's inventory menu is a {@code ContainerPlayer} as in 1.12 (mods cast
     * {@code GuiInventory.inventorySlots} to it: the Galacticraft tab API Techguns ships read its crafting grid and failed, leaving no tabs).
     */
    public static Container of(AbstractContainerMenu modern) {
        if (modern == null) return null;
        ModernMenuView1122 view;
        synchronized (VIEWS) {
            view = VIEWS.computeIfAbsent(modern, m -> m instanceof net.minecraft.world.inventory.InventoryMenu
                    ? new ModernPlayerContainer1122(m) : new ModernContainer1122(m));
        }
        view.mcc$sync();
        return (Container) view;
    }

    @Override
    public AbstractContainerMenu mcc$modern() {
        return modern;
    }

    @Override
    public void mcc$sync() {
        while (inventorySlots.size() < modern.slots.size()) addSlotToContainer(new ViewSlot(modern.slots.get(inventorySlots.size())));
    }

    private static net.minecraft.world.entity.player.Player modern(EntityPlayer player) {
        return ModernMenuView1122.modern(player);
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

    @Override
    public void detectAndSendChanges() {
        modern.broadcastChanges();
        // legacy listeners added to the player's container (inventoryContainer.addListener) hear about changed slots, as 1.12's did
        if (!listeners.isEmpty()) {
            mcc$sync();
            super.detectAndSendChanges();
        }
    }

    @Override
    public void mcc$tickListeners() {
        if (!listeners.isEmpty()) detectAndSendChanges();
    }

    /** The per-tick call of a server player's open menu: runs {@link #detectAndSendChanges} when a legacy mod listens on its view. */
    public static void mcc$tickListeners(AbstractContainerMenu menu) {
        if (menu == null || menu instanceof LegacyMenu1122) return;
        ModernMenuView1122 view;
        synchronized (VIEWS) {
            view = VIEWS.get(menu);
        }
        if (view != null) view.mcc$tickListeners();
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
