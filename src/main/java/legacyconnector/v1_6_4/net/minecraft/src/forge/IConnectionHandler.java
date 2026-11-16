package legacyconnector.v1_6_4.net.minecraft.src.forge;

public interface IConnectionHandler {
    void onConnect(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network);

    void onLogin(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network, legacyconnector.v1_6_4.net.minecraft.network.packet.Packet1Login login);

    void onDisconnect(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network, String message, Object[] args);
}
