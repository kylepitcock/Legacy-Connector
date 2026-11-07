package legacyconnector.v1_6_4.cpw.mods.fml.common.network;

import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet131MapData;
import legacyconnector.v1_7_10.net.minecraft.network.INetHandler;

/** FML 1.6's handler of "tiny packets" (a short tag and a few bytes, see {@code PacketDispatcher.getTinyPacket}). */
public interface ITinyPacketHandler {
    void handle(INetHandler handler, Packet131MapData mapData);
}
