package legacyconnector.v1_12_2.net.minecraft.network.status.server;

/** The server-list reply packet. Mods only reach for its shared {@code GSON} to parse JSON. */
public class SPacketServerInfo {
    public static final com.google.gson.Gson GSON = new com.google.gson.GsonBuilder().create();
}
