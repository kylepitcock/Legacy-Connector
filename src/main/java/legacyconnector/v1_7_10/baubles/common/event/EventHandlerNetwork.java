package legacyconnector.v1_7_10.baubles.common.event;

import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;

/** Baubles' sync entry point. Curios sends the worn slots to the clients by itself whenever they change, so there is nothing to push. */
public class EventHandlerNetwork {
    public static void syncBaubles(EntityPlayer player) {
    }
}
