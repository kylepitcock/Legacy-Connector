package legacyconnector.vb1_7_3.net.minecraft.src;

/**
 * ModLoaderMP's static API. A mod's client and server halves share one process in single player, so a packet has no remote peer:
 * {@code SendPacket} and {@code SendPacketToAll} stamp the packet with the mod's id (as ModLoaderMP does) and end there, the way
 * ModLoaderMP did when no server connection existed.
 */
public final class ModLoaderMp {
    public static final String NAME = "ModLoaderMP";
    public static final String VERSION = "1.7.3";

    private ModLoaderMp() {}

    public static void SendPacket(BaseModMp mod, Packet230ModLoader packet) {
        if (packet == null) throw new IllegalArgumentException("packet cannot be null");
        if (mod != null) packet.modId = mod.getId();
    }

    public static void SendPacketToAll(BaseModMp mod, Packet230ModLoader packet) {
        SendPacket(mod, packet);
    }
}
