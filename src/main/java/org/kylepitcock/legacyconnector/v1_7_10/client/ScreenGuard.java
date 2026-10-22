package org.kylepitcock.legacyconnector.v1_7_10.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A legacy GUI that throws (a tile entity that has not arrived yet, a missing API) is closed instead of crashing the game. */
public final class ScreenGuard {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Gui");

    private ScreenGuard() {}

    /** Runs legacy GUI code; on failure logs it, closes the screen and tells the player. Returns false then. */
    public static boolean run(Object screen, Runnable code) {
        try {
            code.run();
            return true;
        } catch (RuntimeException | LinkageError e) {
            LOG.error("The legacy GUI {} failed and was closed", screen.getClass().getName(), e);
            var mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.screen == screen) mc.setScreen(null);
                if (mc.player != null) mc.player.displayClientMessage(Component.literal("A legacy GUI failed and was closed: " + e), false);
            });
            return false;
        }
    }
}
