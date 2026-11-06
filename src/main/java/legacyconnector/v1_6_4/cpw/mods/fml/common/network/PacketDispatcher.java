package legacyconnector.v1_6_4.cpw.mods.fml.common.network;

import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet131MapData;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet250CustomPayload;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.Player;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Network164;

/** FML 1.6's helper for sending packets to players, dimensions, everyone, or the server. */
public final class PacketDispatcher {
    private PacketDispatcher() {}

    /** A custom payload packet for {@code channel} carrying {@code data}. */
    public static Packet250CustomPayload getPacket(String channel, byte[] data) {
        return new Packet250CustomPayload(channel, data);
    }

    /** A tiny packet for the mod {@code mod} (its instance), with a tag the receiving {@link ITinyPacketHandler} sees as {@code uniqueID}. */
    public static Packet131MapData getTinyPacket(Object mod, short tag, byte[] data) {
        return Network164.tinyPacket(mod, tag, data);
    }

    public static void sendPacketToServer(Packet packet) {
        Network164.sendToServer(packet);
    }

    public static void sendPacketToPlayer(Packet packet, Player player) {
        Network164.sendToPlayer(packet, player);
    }

    public static void sendPacketToAllPlayers(Packet packet) {
        Network164.sendToAll(packet);
    }

    public static void sendPacketToAllInDimension(Packet packet, int dimension) {
        Network164.sendToDimension(packet, dimension);
    }

    /** To the players of {@code dimension} within {@code range} blocks of the point. */
    public static void sendPacketToAllAround(double x, double y, double z, double range, int dimension, Packet packet) {
        Network164.sendToAllAround(x, y, z, range, dimension, packet);
    }
}
