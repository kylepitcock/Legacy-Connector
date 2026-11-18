package legacyconnector.v1_7_10.net.minecraft.client.multiplayer;

/** A server address of the multiplayer list ("host", "host:port", "[::1]:port"); the port defaults to 25565. */
public class ServerAddress {
    private final String ipAddress;
    private final int serverPort;

    private ServerAddress(String ip, int port) {
        this.ipAddress = ip;
        this.serverPort = port;
    }

    public String getIP() {
        return ipAddress;
    }

    public int getPort() {
        return serverPort;
    }

    /** Parses what a player typed (the modern game's parser: IPv6 in brackets, port after the last colon); never fails. */
    public static ServerAddress func_78860_a(String text) {
        var parsed = net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(text == null ? "" : text);
        return new ServerAddress(parsed.getHost(), parsed.getPort());
    }

    /** The modern address this one names (for the connect screen). */
    public net.minecraft.client.multiplayer.resolver.ServerAddress mcc$modern() {
        return new net.minecraft.client.multiplayer.resolver.ServerAddress(ipAddress, serverPort);
    }
}
