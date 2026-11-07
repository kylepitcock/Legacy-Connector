package legacyconnector.v1_6_4.cpw.mods.fml.common.network;

import legacyconnector.v1_6_4.net.minecraft.network.INetworkManager;
import legacyconnector.v1_6_4.net.minecraft.network.packet.Packet250CustomPayload;
import legacyconnector.v1_7_10.cpw.mods.fml.common.network.Player;

/** FML 1.6's handler of a custom payload channel (registered with {@code NetworkRegistry.registerChannel} or {@code @NetworkMod}). */
public interface IPacketHandler {
    void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player);
}
