package legacyconnector.v1_6_4.net.minecraft.src.forge;

public interface IPacketHandler {
    void onPacketData(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network, String channel, byte[] data);
}
