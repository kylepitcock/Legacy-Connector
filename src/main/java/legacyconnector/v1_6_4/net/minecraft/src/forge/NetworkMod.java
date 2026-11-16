package legacyconnector.v1_6_4.net.minecraft.src.forge;

/**
 * Forge 3.x's {@code NetworkMod}: a ModLoader mod that is told by Forge whether the client and the server must both have it and receives
 * the packets sent with {@code MinecraftForge.sendPacket(network, mod, id, bytes)}.
 */
public abstract class NetworkMod extends legacyconnector.v1_6_4.net.minecraft.src.BaseMod {
    public NetworkMod() {}

    public boolean clientSideRequired() {
        return false;
    }

    public boolean serverSideRequired() {
        return false;
    }

    public void onPacketData(legacyconnector.v1_6_4.net.minecraft.network.INetworkManager network, short packetId, byte[] data) {}
}
