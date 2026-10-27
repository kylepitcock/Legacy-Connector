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

    private static final java.util.concurrent.atomic.AtomicBoolean WARNED = new java.util.concurrent.atomic.AtomicBoolean();

    /** Set by the GUI bridge; until then the request is logged once. */
    public static volatile OpenGui OPEN_GUI = (player, mod, id, world, x, y, z) -> {
        if (WARNED.compareAndSet(false, true)) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Entities").warn("A 1.12.2 mod opened GUI {} of {}, but mod GUIs cannot be opened yet", id, mod);
        }
    };

    public static void openGui(EntityPlayer player, Object mod, int id, World world, int x, int y, int z) {
        try {
            OPEN_GUI.open(player, mod, id, world, x, y, z);
        } finally {
            LiveStacks1122.flushAll();
        }
    }
}
