package org.kylepitcock.legacyconnector.v1_12_2.gui;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.ClickType;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;

/**
 * A legacy container that views a modern menu ({@link ModernContainer1122}, and {@link ModernPlayerContainer1122} for the player's own
 * inventory menu, which legacy code expects to be a {@code ContainerPlayer}). The shared behaviour is here, the containers call it.
 */
public interface ModernMenuView1122 {
    AbstractContainerMenu mcc$modern();

    /** Adds views for modern slots added since the last call (menus add slots in their constructors, a few later). */
    void mcc$sync();

    /** Runs the view's detectAndSendChanges when a legacy listener was added to it. */
    void mcc$tickListeners();

    static net.minecraft.world.entity.player.Player modern(EntityPlayer player) {
        return player != null && player.mcc$entity instanceof net.minecraft.world.entity.player.Player p ? p : null;
    }

    static boolean canInteract(AbstractContainerMenu modern, EntityPlayer player) {
        var p = modern(player);
        return p == null || modern.stillValid(p);
    }

    static ItemStack transfer(AbstractContainerMenu modern, EntityPlayer player, int index) {
        var p = modern(player);
        return p == null ? ItemStack.EMPTY : Stacks1122.fromModern(modern.quickMoveStack(p, index));
    }

    /** A click as the player makes it: on the client through the game mode (the server is told), elsewhere on the menu directly. */
    static ItemStack click(Container view, AbstractContainerMenu modern, int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        ItemStack before = slotId >= 0 && slotId < view.inventorySlots.size() ? view.inventorySlots.get(slotId).getStack().copy() : ItemStack.EMPTY;
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
}
