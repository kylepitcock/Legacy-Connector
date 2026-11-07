package legacyconnector.v1_7_10.net.minecraft.network;

/** Marker for the legacy network handlers handed to mods (FMLNetworkEvent.handler, MessageContext). */
public interface INetHandler {
    /** 1.6.4's {@code NetHandler.getPlayer()}: the player this handler belongs to (null where there is none). */
    default legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer getPlayer() {
        return null;
    }
}
