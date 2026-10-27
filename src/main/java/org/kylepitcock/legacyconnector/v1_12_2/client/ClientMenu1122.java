package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.GuiBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.gui.LegacyMenu1122;

/**
 * The menu behind a legacy container GUI that the client displays itself ({@code mc.displayGuiScreen(new GuiInventory(player))}, a mod's
 * own inventory screen opened from a key or a GuiOpenEvent). 1.12 ran such a screen's clicks on {@code player.openContainer}, which is
 * the player's inventory unless the server opened a container; this menu only lays out and draws the GUI's slots, and every click goes to
 * the player's open modern menu by slot number (the inventory's slots are numbered as 1.12's ContainerPlayer). The stack on the cursor is
 * that menu's.
 */
public final class ClientMenu1122 extends LegacyMenu1122 {
    private final net.minecraft.world.entity.player.Player owner;

    private ClientMenu1122(GuiContainer gui, net.minecraft.world.entity.player.Player player) {
        super(GuiBridge1122.menuType(), gui.inventorySlots.windowId, player.getInventory(), gui.inventorySlots, gui);
        this.owner = player;
    }

    /** The modern screen showing a client-displayed legacy container GUI, or null when there is no player to show it to. */
    public static net.minecraft.client.gui.screens.Screen screen(GuiContainer gui) {
        var player = Minecraft.getInstance().player;
        if (player == null || gui.inventorySlots == null || GuiBridge1122.menuType() == null) return null;
        return new LegacyContainerScreen1122(new ClientMenu1122(gui, player), player.getInventory(), Component.empty());
    }

    private net.minecraft.world.inventory.AbstractContainerMenu open() {
        var open = owner.containerMenu;
        return open == null || open == this ? owner.inventoryMenu : open;
    }

    @Override
    public ItemStack getCarried() {
        return owner == null ? super.getCarried() : open().getCarried();
    }

    @Override
    public void setCarried(ItemStack stack) {
        if (owner == null) super.setCarried(stack);
        else open().setCarried(stack);
    }

    /** A click on the player's open menu, sent to the server as the game's own screens send them. */
    public static void click(int containerId, int slotId, int button, net.minecraft.world.inventory.ClickType type) {
        var mc = Minecraft.getInstance();
        if (mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleInventoryMouseClick(containerId, slotId, button, type, mc.player);
    }
}
