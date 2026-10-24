package legacyconnector.v1_7_10.net.minecraft.network;

/** Opaque connection handle passed to legacy callbacks; carries no API surface. */
public class NetworkManager {
    public static final NetworkManager MCC_DUMMY = new NetworkManager();

    public boolean isLocalChannel() {
        return false;
    }

    public boolean isChannelOpen() {
        return true;
    }

    public java.net.SocketAddress getSocketAddress() {
        return null;
    }

    public io.netty.channel.Channel channel() {
        return null;
    }

    public void scheduleOutboundPacket(legacyconnector.v1_7_10.net.minecraft.network.Packet packet, io.netty.util.concurrent.GenericFutureListener... listeners) {
    }
}
