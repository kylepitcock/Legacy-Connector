package legacyconnector.v1_7_10.net.minecraft.inventory;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.InventoryPlayer;

/**
 * The container of a player's own inventory screen ({@code player.inventoryContainer}). Legacy code uses it to
 * force a resync after changing the inventory ({@code detectAndSendChanges}); that maps to the modern menu.
 */
public class ContainerPlayer extends Container {
    private final EntityPlayer owner;

    public ContainerPlayer(InventoryPlayer inventory, boolean local, EntityPlayer player) {
        this.owner = player;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public void detectAndSendChanges() {
        if (owner != null && owner.mcc$player() != null) owner.mcc$player().inventoryMenu.broadcastChanges();
    }
}
