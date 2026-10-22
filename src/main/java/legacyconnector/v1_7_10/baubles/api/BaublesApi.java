package legacyconnector.v1_7_10.baubles.api;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.inventory.IInventory;
import org.kylepitcock.legacyconnector.compat.curios.CuriosBaubles;

/**
 * Baubles 1.0 entry point, re-implemented on Curios. Only linked when the Curios bridge is active (Curios installed),
 * in which case the legacy Baubles mod is not loaded at all.
 */
public class BaublesApi {
    /** Slots 0 amulet, 1-2 rings, 3 belt - Baubles' layout, backed by the player's Curios slots. */
    public static IInventory getBaubles(EntityPlayer player) {
        return player == null ? null : CuriosBaubles.inventory(player);
    }
}
