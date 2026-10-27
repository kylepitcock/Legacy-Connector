package org.kylepitcock.legacyconnector.v1_12_2.gui;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.ClickType;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.inventory.IContainerListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * Modern menu hosting a 1.12.2 {@link Container}. Clicking, dragging and slot syncing are modern (through {@link LegacySlot1122}s);
 * shift-click, validity, closing, menu buttons and progress bars call the legacy container. Progress bars travel as vanilla container
 * data packets, which arrive here in {@link #setData} and go to the client container's {@code updateProgressBar}.
 */
public class LegacyMenu1122 extends AbstractContainerMenu {
    private final Container legacy;
    /** Client only: the legacy GuiContainer this menu was created for. */
    private final Object clientGui;
    private final Player player;
    private boolean listenerAdded;
    /** Whether the legacy container class overrides slotClick: every click then goes through it (it may swallow or rewrite them). */
    private final boolean overridesSlotClick;
    private boolean inSlotClick;

    public LegacyMenu1122(MenuType<?> type, int windowId, Inventory playerInventory, Container legacy, Object clientGui) {
        super(type, windowId);
        this.legacy = legacy;
        this.clientGui = clientGui;
        this.player = playerInventory.player;
        this.overridesSlotClick = overrides(legacy, "slotClick", int.class, int.class, ClickType.class, EntityPlayer.class);
        legacy.windowId = windowId;
        legacy.mcc$menu = this;
        for (var slot : legacy.inventorySlots) addSlot(new LegacySlot1122(slot));
    }

    /** True if a class below the shim overrides the named method. */
    public static boolean overrides(Object obj, String name, Class<?>... params) {
        for (Class<?> c = obj.getClass(); c != null && !c.getName().startsWith("legacyconnector."); c = c.getSuperclass()) {
            try {
                c.getDeclaredMethod(name, params);
                return true;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return false;
    }

    public Container legacy() {
        return legacy;
    }

    public Object clientGui() {
        return clientGui;
    }

    private static EntityPlayer legacyPlayer(Player p) {
        return EntityBridge1122.wrap(p) instanceof EntityPlayer ep ? ep : null;
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        if (!overridesSlotClick || inSlotClick) {
            super.clicked(slotId, button, type, player);
            return;
        }
        inSlotClick = true;
        try {
            legacy.slotClick(slotId, button, ClickType.mcc$of(type), legacyPlayer(player));
        } finally {
            inSlotClick = false;
        }
    }

    /** The modern click algorithm, for {@code Container.slotClick} to fall back to. */
    public void mcc$modernClick(int slotId, int button, ClickType type) {
        super.clicked(slotId, button, type == null ? net.minecraft.world.inventory.ClickType.PICKUP : type.mcc$modern(), player);
    }

    /**
     * The client's legacy container can have other slots than the server's (a tile entity that has not arrived yet when the GUI was
     * built): the slots that exist are filled and the rest ignored instead of disconnecting.
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

    /** A progress bar value from the server's {@code sendWindowProperty}. */
    @Override
    public void setData(int id, int value) {
        legacy.updateProgressBar(id, value);
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        EntityPlayer p = legacyPlayer(player);
        if (p == null) return net.minecraft.world.item.ItemStack.EMPTY;
        var result = legacy.transferStackInSlot(p, index);
        return result == null ? net.minecraft.world.item.ItemStack.EMPTY : Stacks1122.toModern(result);
    }

    @Override
    public boolean stillValid(Player player) {
        EntityPlayer p = legacyPlayer(player);
        return p != null && legacy.canInteractWith(p);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        EntityPlayer p = legacyPlayer(player);
        if (p != null) {
            if (p instanceof IContainerListener l) legacy.removeListener(l);
            legacy.onContainerClosed(p);
        }
    }

    @Override
    public void broadcastChanges() {
        if (player instanceof ServerPlayer sp && !listenerAdded) {
            listenerAdded = true;
            if (EntityBridge1122.wrap(sp) instanceof IContainerListener l) {
                try {
                    legacy.addListener(l);
                } catch (IllegalArgumentException alreadyListening) {
                    // the mod added the player itself
                }
            }
        }
        super.broadcastChanges();
        legacy.detectAndSendChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        EntityPlayer p = legacyPlayer(player);
        return p != null && legacy.enchantItem(p, id);
    }
}
