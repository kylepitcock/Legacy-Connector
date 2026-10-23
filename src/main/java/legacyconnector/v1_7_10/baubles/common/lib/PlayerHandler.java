package legacyconnector.v1_7_10.baubles.common.lib;

import legacyconnector.v1_7_10.baubles.common.container.InventoryBaubles;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** Baubles' internal per-player store, as the Curios-backed bridge. */
public class PlayerHandler {
    public static InventoryBaubles getPlayerBaubles(EntityPlayer player) {
        return new InventoryBaubles(player);
    }

    public static void setPlayerBaubles(EntityPlayer player, InventoryBaubles inventory) {
    }

    public static void clearPlayerBaubles(EntityPlayer player) {
    }

    public static void loadPlayerBaubles(EntityPlayer player, java.io.File file1, java.io.File file2) {
    }

    public static void savePlayerBaubles(EntityPlayer player, java.io.File file1, java.io.File file2) {
    }
}
