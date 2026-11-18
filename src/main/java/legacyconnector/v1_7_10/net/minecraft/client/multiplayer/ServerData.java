package legacyconnector.v1_7_10.net.minecraft.client.multiplayer;

/** 1.7.10's entry of the multiplayer server list. */
public class ServerData {
    public String serverName;
    public String serverIP;
    public String populationInfo;
    public String serverMOTD;
    public long pingToServer;
    public int field_82821_f = 5;
    public String gameVersion = "1.7.10";
    public boolean field_78841_f;
    public String field_147412_i;
    private boolean isLanServer;

    public ServerData(String name, String ip) {
        this.serverName = name;
        this.serverIP = ip;
    }

    /** The form with the LAN flag (an entry for a world shared on the local network). */
    public ServerData(String name, String ip, boolean lan) {
        this(name, ip);
        this.isLanServer = lan;
    }

    /** The server's icon as a base64 PNG (what its status reply carried), or null. */
    public String getBase64EncodedIconData() {
        return field_147412_i;
    }

    public void setBase64EncodedIconData(String icon) {
        this.field_147412_i = icon;
    }

    public boolean isLanServer() {
        return isLanServer;
    }

    /** The SRG name of {@link #copyFrom} (1.7.10's mappings name it {@code copyFrom}); shim code still calls this one. */
    public void func_152583_a(ServerData other) {
        copyFrom(other);
    }

    public void copyFrom(ServerData other) {
        this.serverIP = other.serverIP;
        this.serverName = other.serverName;
        this.field_147412_i = other.field_147412_i;
    }
}
