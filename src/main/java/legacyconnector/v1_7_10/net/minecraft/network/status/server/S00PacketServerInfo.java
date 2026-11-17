package legacyconnector.v1_7_10.net.minecraft.network.status.server;

import com.google.gson.JsonObject;
import java.io.IOException;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_7_10.net.minecraft.network.ServerStatusResponse;

/**
 * The server list reply: description, player counts and the version. The host answers server-list pings itself; this carries what a mod
 * built or read (its JSON form is the 1.7.10 wire format).
 */
public class S00PacketServerInfo extends Packet {
    private ServerStatusResponse field_149296_b;

    public S00PacketServerInfo() {
    }

    public S00PacketServerInfo(ServerStatusResponse response) {
        this.field_149296_b = response;
    }

    public ServerStatusResponse func_149294_c() {
        return field_149296_b;
    }

    @Override
    public boolean hasPriority() {
        return true;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        JsonObject json = com.google.gson.JsonParser.parseString(data.readStringFromBuffer(32767)).getAsJsonObject();
        ServerStatusResponse response = new ServerStatusResponse();
        if (json.has("description")) {
            response.setServerDescription(new legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText(json.get("description").isJsonPrimitive()
                    ? json.get("description").getAsString() : json.get("description").toString()));
        }
        if (json.has("players")) {
            JsonObject players = json.getAsJsonObject("players");
            response.setPlayerCountData(new ServerStatusResponse.PlayerCountData(players.get("max").getAsInt(), players.get("online").getAsInt()));
        }
        if (json.has("version")) {
            JsonObject version = json.getAsJsonObject("version");
            response.setProtocolVersionInfo(new ServerStatusResponse.MinecraftProtocolVersionIdentifier(version.get("name").getAsString(),
                    version.get("protocol").getAsInt()));
        }
        field_149296_b = response;
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        JsonObject json = new JsonObject();
        if (field_149296_b != null) {
            if (field_149296_b.getServerDescription() != null) json.addProperty("description", field_149296_b.getServerDescription().getUnformattedText());
            ServerStatusResponse.PlayerCountData players = field_149296_b.getPlayerCountData();
            if (players != null) {
                JsonObject p = new JsonObject();
                p.addProperty("max", players.getMaxPlayers());
                p.addProperty("online", players.getOnlinePlayerCount());
                json.add("players", p);
            }
            ServerStatusResponse.MinecraftProtocolVersionIdentifier version = field_149296_b.getProtocolVersionInfo();
            if (version != null) {
                JsonObject v = new JsonObject();
                v.addProperty("name", version.getName());
                v.addProperty("protocol", version.getProtocol());
                json.add("version", v);
            }
        }
        data.writeStringToBuffer(json.toString());
    }
}
