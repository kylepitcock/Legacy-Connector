package org.kylepitcock.legacyconnector.v1_12_2.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Player actions whose bridge lives in another part of the layer (opening mod GUIs belongs to the GUI bridge). */
public final class PlayerHooks1122 {
    private PlayerHooks1122() {}

    /** {@code EntityPlayer.openGui}: opens a mod's GUI through its IGuiHandler. */
    public interface OpenGui {
        void open(EntityPlayer player, Object mod, int id, World world, int x, int y, int z);
    }

    /** The GUI bridge: a container GUI from the server, a client-only one on the client. */
    public static volatile OpenGui OPEN_GUI = org.kylepitcock.legacyconnector.v1_12_2.bridge.GuiBridge1122::openGui;

    public static void openGui(EntityPlayer player, Object mod, int id, World world, int x, int y, int z) {
        try {
            OPEN_GUI.open(player, mod, id, world, x, y, z);
        } finally {
            LiveStacks1122.flushAll();
        }
    }
}
