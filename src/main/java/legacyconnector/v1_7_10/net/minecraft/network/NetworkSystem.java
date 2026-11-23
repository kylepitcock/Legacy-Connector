package legacyconnector.v1_7_10.net.minecraft.network;

/**
 * The server's listening endpoints (vanilla's {@code NetworkSystem}). The host owns the sockets, so a mod asking to open a LAN or
 * local endpoint gets no new one; its calls are accepted and do nothing.
 */
public class NetworkSystem {
    public static final NetworkSystem INSTANCE = new NetworkSystem();

    /** Opens a listening socket on an address: the host's own listener serves the game, no second one is opened. */
    public void addLanEndpoint(java.net.InetAddress address, int port) throws java.io.IOException {
    }

    /** Opens the in-process endpoint of the integrated server: the host's local channel is already open. */
    public java.net.SocketAddress addLocalEndpoint() {
        return new java.net.InetSocketAddress(java.net.InetAddress.getLoopbackAddress(), 0);
    }

    /** Processes the connections' queued packets: the host does it every tick. */
    public void networkTick() {
    }

    public void terminateEndpoints() {
    }
}
