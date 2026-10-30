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

    public ServerData(String name, String ip) {
        this.serverName = name;
        this.serverIP = ip;
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
