package org.kylepitcock.legacyconnector.v1_7_10.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/** Legacy achievements and notifications shown as toasts in the top right corner. */
public final class LegacyToasts {
    private LegacyToasts() {}

    /** True when the player is the one sitting at this client (single player or the local player on a server). */
    public static boolean isLocal(java.util.UUID player) {
        var local = Minecraft.getInstance().player;
        return local != null && local.getUUID().equals(player);
    }

    public static void achievement(String name) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> SystemToast.add(mc.getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("Achievement get!"), Component.literal(name)));
    }
}
