package legacyconnector.v1_6_4.facade.net.minecraft.client.network;

import legacyconnector.v1_6_4.net.minecraft.network.INetworkManager;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Manager164;

/** Facade of the 1.7.10 shim's {@code NetHandlerPlayClient} for 1.6.4 mods ({@code NetClientHandler}): the connection as an {@link INetworkManager}. Never instantiated. */
public final class NetHandlerPlayClient {
    private NetHandlerPlayClient() {}

    /** NetClientHandler.getNetManager() */
    public static INetworkManager getNetManager(legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient self) {
        return Manager164.of(self);
    }

    /** NetClientHandler.handleKickDisconnect(Packet255KickDisconnect): leaves the world with the server's reason. */
    public static void handleKickDisconnect(legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient self,
            legacyconnector.v1_6_4.net.minecraft.network.packet.Packet255KickDisconnect packet) {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection != null && packet != null) {
            connection.getConnection().disconnect(net.minecraft.network.chat.Component.literal(packet.reason == null ? "" : packet.reason));
        }
    }

    /** NetClientHandler.netManager */
    public static INetworkManager mcc$get$netManager(legacyconnector.v1_7_10.net.minecraft.client.network.NetHandlerPlayClient self) {
        return Manager164.of(self);
    }
}
