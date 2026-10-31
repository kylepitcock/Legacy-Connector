package org.kylepitcock.legacyconnector.v1_7_10.gui;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.Container;
import legacyconnector.v1_7_10.net.minecraft.inventory.ICrafting;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.GuiBridge;

/**
 * Modern menu hosting a 1.7.10 {@link Container}. Clicking, dragging and slot syncing are done by modern code
 * through {@link LegacySlot}s; shift-click, validity, closing and progress bars call the legacy container.
 */
public class LegacyMenu extends AbstractContainerMenu {
    private final Container legacy;
    /** Client only: the legacy GuiContainer this menu was created for. */
    private final Object clientGui;
    private final Player player;
    private boolean crafterAdded;
    /** Whether the legacy container class overrides slotClick: every click then goes through it (it may swallow or rewrite them). */
    private final boolean overridesSlotClick;
    private boolean inSlotClick;

    public LegacyMenu(MenuType<?> type, int windowId, Inventory playerInventory, Container legacy, Object clientGui) {
        super(type, windowId);
        this.legacy = legacy;
        this.clientGui = clientGui;
        this.player = playerInventory.player;
        this.overridesSlotClick = org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockAdapter.overrides(legacy, "slotClick", int.class, int.class, int.class, EntityPlayer.class);
        legacy.windowId = windowId;
        legacy.mcc$menu = this;
        for (Object o : legacy.inventorySlots) addSlot(new LegacySlot((legacyconnector.v1_7_10.net.minecraft.inventory.Slot) o));
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        if (!overridesSlotClick || inSlotClick) {
            super.clicked(slotId, button, type, player);
            return;
        }
        inSlotClick = true;
        try {
            legacy.slotClick(slotId, button, type.ordinal(), legacyPlayer(player));
        } finally {
            inSlotClick = false;
        }
    }

    /** The modern click algorithm, for {@code Container.slotClick} to fall back to. */
    public void mcc$modernClick(int slotId, int button, int mode, EntityPlayer legacyPlayer) {
        var types = net.minecraft.world.inventory.ClickType.values();
        super.clicked(slotId, button, mode >= 0 && mode < types.length ? types[mode] : net.minecraft.world.inventory.ClickType.PICKUP, player);
    }

    /** The mod adds the player to the container's listeners itself (it opened the container by hand); the menu must not add another. */
    public void mcc$crafterManaged() {
        crafterAdded = true;
    }

    /** Client: the screen was built from a mod's own packet and the server's window id has not arrived yet. */
    private boolean detached;

    public boolean mcc$detached() {
        return detached;
    }

    public void mcc$setDetached(boolean detached) {
        this.detached = detached;
    }

    private static final java.lang.reflect.Field CONTAINER_ID = containerIdField();

    private static java.lang.reflect.Field containerIdField() {
        try {
            var f = AbstractContainerMenu.class.getField("containerId");
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Gives a detached menu the window id the server opened its container under ({@code containerId} is final). */
    public boolean mcc$setWindowId(int id) {
        if (CONTAINER_ID == null) return false;
        try {
            CONTAINER_ID.setInt(this, id);
            legacy.windowId = id;
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    public Container legacy() {
        return legacy;
    }

    /**
     * The client's legacy container can have other slots than the server's (a tile entity that has not arrived yet when the GUI
     * was built): the server's content packet then names slots that do not exist here. Modern code throws and disconnects; the
     * slots that exist are filled and the rest ignored.
     */
    @Override
    public void initializeContents(int stateId, List<net.minecraft.world.item.ItemStack> items, net.minecraft.world.item.ItemStack carried) {
        for (int i = 0; i < items.size() && i < slots.size(); i++) getSlot(i).set(items.get(i));
        setCarried(carried);
    }

    @Override
    public void setItem(int slot, int stateId, net.minecraft.world.item.ItemStack stack) {
        if (slot >= 0 && slot < slots.size()) super.setItem(slot, stateId, stack);
    }

    public Object clientGui() {
        return clientGui;
    }

    private EntityPlayer legacyPlayer(Player p) {
        return (EntityPlayer) EntityBridge.wrap(p);
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = legacy.transferStackInSlot(legacyPlayer(player), index);
        return ItemStack.mcc$toModern(result);
    }

    @Override
    public boolean stillValid(Player player) {
        return legacy.canInteractWith(legacyPlayer(player));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        legacy.onContainerClosed(legacyPlayer(player));
    }

    @Override
    public void broadcastChanges() {
        if (player instanceof ServerPlayer sp && !crafterAdded) {
            crafterAdded = true;
            legacy.addCraftingToCrafters(org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(sp) instanceof ICrafting c ? c : new Crafter(sp));
        }
        super.broadcastChanges();
        legacy.detectAndSendChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return legacy.enchantItem(legacyPlayer(player), id);
    }

    /** Server-side listener: slot syncing is modern; progress bars travel through our own payload. */
    private record Crafter(ServerPlayer player) implements ICrafting {
        @Override
        public void sendContainerAndContentsToPlayer(Container container, List items) {
        }

        @Override
        public void sendSlotContents(Container container, int slot, ItemStack stack) {
        }

        @Override
        public void sendProgressBarUpdate(Container container, int id, int value) {
            GuiBridge.sendProgressBar(player, container.windowId, id, value);
        }
    }
}
