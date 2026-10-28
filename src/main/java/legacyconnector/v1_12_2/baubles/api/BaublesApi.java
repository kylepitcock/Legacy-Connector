package legacyconnector.v1_12_2.baubles.api;

import legacyconnector.v1_12_2.baubles.api.cap.IBaublesItemHandler;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.compat.curios.CuriosBaubles1122;

/** Baubles 1.12's API: a player's Baubles slots are a view over their Curios slots (the bridge is active only with Curios). */
public class BaublesApi {
    public static IBaublesItemHandler getBaublesHandler(EntityPlayer player) {
        return player == null ? null : CuriosBaubles1122.handler(player);
    }

    @Deprecated
    public static IInventory getBaubles(EntityPlayer player) {
        return player == null ? null : CuriosBaubles1122.inventory(player);
    }

    /** The first Baubles slot holding the item, or -1. */
    public static int isBaubleEquipped(EntityPlayer player, Item bauble) {
        IBaublesItemHandler handler = getBaublesHandler(player);
        if (handler == null) return -1;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == bauble) return i;
        }
        return -1;
    }
}
