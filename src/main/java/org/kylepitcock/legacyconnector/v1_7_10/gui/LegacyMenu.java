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

    public LegacyMenu(MenuType<?> type, int windowId, Inventory playerInventory, Container legacy, Object clientGui) {
        super(type, windowId);
        this.legacy = legacy;
        this.clientGui = clientGui;
        this.player = playerInventory.player;
        legacy.windowId = windowId;
        legacy.mcc$menu = this;
        for (Object o : legacy.inventorySlots) addSlot(new LegacySlot((legacyconnector.v1_7_10.net.minecraft.inventory.Slot) o));
    }

    public Container legacy() {
        return legacy;
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
            legacy.addCraftingToCrafters(new Crafter(sp));
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
