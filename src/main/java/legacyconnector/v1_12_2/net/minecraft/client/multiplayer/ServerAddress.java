package legacyconnector.v1_12_2.net.minecraft.client.multiplayer;

import java.net.IDN;

/**
 * A server address as typed ("host", "host:port", "[ipv6]:port"): host and port, the port defaulting to 25565. The 1.12 client also looked
 * up an SRV record for the default port; the layer leaves that to the host's own connection code, so the parse here is the plain one.
 */
public class ServerAddress {
    private final String ipAddress;
    private final int serverPort;

    private ServerAddress(String address, int port) {
        this.ipAddress = address;
        this.serverPort = port;
    }

    public String getIP() {
        try {
            return IDN.toASCII(this.ipAddress);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    public int getPort() {
        return this.serverPort;
    }

    public static ServerAddress fromString(String addrString) {
        if (addrString == null) return null;
        String[] parts = addrString.split(":");
        if (addrString.startsWith("[")) {
            int close = addrString.indexOf("]");
            if (close > 0) {
                String host = addrString.substring(1, close);
                String rest = addrString.substring(close + 1).trim();
                if (rest.startsWith(":") && !rest.isEmpty()) parts = new String[] {host, rest.substring(1)};
                else parts = new String[] {host};
            }
        }
        if (parts.length > 2) parts = new String[] {addrString};
        String host = parts[0];
        int port = parts.length > 1 ? getInt(parts[1], 25565) : 25565;
        return new ServerAddress(host, port);
    }

    private static int getInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
