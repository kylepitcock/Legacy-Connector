package legacyconnector.vb1_7_3.net.minecraft.client.network;

import legacyconnector.vb1_7_3.net.minecraft.network.NetworkHandler;
import legacyconnector.vb1_7_3.net.minecraft.network.packet.Packet;

/**
 * Beta's {@code NetClientHandler}: {@code addToSendQueue} hands a packet to the server. A packet of a registered id is written with its
 * own {@code writePacketData} and run on the server ({@code processPacket}) with the sending player as the callback's player.
 */
public class ClientNetworkHandler extends NetworkHandler {
    private static final ClientNetworkHandler INSTANCE = new ClientNetworkHandler();

    protected ClientNetworkHandler() {}

    public static ClientNetworkHandler mcc$instance() { return INSTANCE; }

    @Override public boolean isServerSide() { return false; }

    public void sendPacket(Packet packet) {
        org.kylepitcock.legacyconnector.vb1_7_3.PacketsB173.toServer(packet);
    }
}
