package org.kylepitcock.legacyconnector.v1_12_2.entity;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.ClickType;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.Slot;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.gui.LegacyMenu1122;

/**
 * 1.12.2 {@link Container} view of a modern menu that is not a legacy container: the player's inventory menu
 * ({@code player.inventoryContainer}) and vanilla menus the player has open ({@code player.openContainer} while a chest or furnace is
 * open). Its slots are views of the modern slots; clicks, shift-clicks, syncing and closing are the modern menu's.
 */
public final class MenuView1122 extends Container {
    private static final Map<AbstractContainerMenu, MenuView1122> VIEWS = new WeakHashMap<>();
    private final AbstractContainerMenu menu;
    private int builtFor = -1;

    private MenuView1122(AbstractContainerMenu menu) {
        this.menu = menu;
        this.windowId = menu.containerId;
        this.mcc$menu = menu;
        refreshSlots();
    }

    /** The legacy container of a modern menu: a legacy menu's own container, otherwise a view (one per menu). */
    public static Container of(AbstractContainerMenu menu) {
        if (menu == null) return null;
        if (menu instanceof LegacyMenu1122 legacy) return legacy.legacy();
        synchronized (VIEWS) {
            MenuView1122 view = VIEWS.computeIfAbsent(menu, MenuView1122::new);
            view.refreshSlots();
            return view;
        }
    }

    /** The modern menu behind a legacy container (its hosting menu), or null when it is not open anywhere. */
    public static AbstractContainerMenu modern(Container container) {
        return container != null && container.mcc$menu instanceof AbstractContainerMenu m ? m : null;
    }

    public AbstractContainerMenu mcc$modernMenu() {
        return menu;
    }

    private void refreshSlots() {
        if (builtFor == menu.slots.size()) return;
        inventorySlots.clear();
        inventoryItemStacks.clear();
        for (net.minecraft.world.inventory.Slot s : menu.slots) addSlotToContainer(new SlotView(s));
        builtFor = menu.slots.size();
    }

    private static net.minecraft.world.entity.player.Player modern(EntityPlayer player) {
        return player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p ? p : null;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        var p = modern(player);
        return p == null || menu.stillValid(p);
    }

    @Override
    public void detectAndSendChanges() {
        Stacks1122.flushViews();
        menu.broadcastChanges();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        var p = modern(player);
        return p == null ? ItemStack.EMPTY : Stacks1122.fromModern(menu.quickMoveStack(p, index));
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        var p = modern(player);
        ItemStack before = slotId >= 0 && slotId < inventorySlots.size() ? inventorySlots.get(slotId).getStack().copy() : ItemStack.EMPTY;
        if (p != null) menu.clicked(slotId, dragType, clickType == null ? net.minecraft.world.inventory.ClickType.PICKUP : clickType.mcc$modern(), p);
        return before;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        var p = modern(player);
        if (p != null) menu.removed(p);
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return !(slot instanceof SlotView v) || menu.canTakeItemForPickAll(Stacks1122.toModern(stack), v.modern);
    }

    @Override
    public void putStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < menu.slots.size()) menu.getSlot(slot).set(Stacks1122.toModernLive(stack));
    }

    /** A legacy slot over a modern slot. */
    public static final class SlotView extends Slot {
        final net.minecraft.world.inventory.Slot modern;

        SlotView(net.minecraft.world.inventory.Slot modern) {
            super(ModernInventory1122.of(modern.container), modern.getContainerSlot(), modern.x, modern.y);
            this.modern = modern;
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
            Stacks1122.flushViews();
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
            return !(player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) || modern.mayPickup(p);
        }

        @Override
        public ItemStack onTake(EntityPlayer player, ItemStack stack) {
            if (player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p) modern.onTake(p, Stacks1122.toModern(stack));
            return stack;
        }

        @Override
        public boolean isEnabled() {
            return modern.isActive();
        }
    }
}
