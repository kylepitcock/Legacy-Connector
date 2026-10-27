package legacyconnector.v1_12_2.net.minecraft.client.multiplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * The saved multiplayer servers ({@code servers.dat}). Loading reads the game's list and saving writes this one back through it, so the
 * entries a mod adds show on the multiplayer screen (and hidden entries the game keeps are not lost).
 */
public class ServerList {
    private final List<ServerData> servers = new ArrayList<>();

    public ServerList(legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc) {
        loadServerList();
    }

    private static net.minecraft.client.multiplayer.ServerList modern() {
        return new net.minecraft.client.multiplayer.ServerList(net.minecraft.client.Minecraft.getInstance());
    }

    public void loadServerList() {
        servers.clear();
        try {
            var modern = modern();
            modern.load();
            for (int i = 0; i < modern.size(); i++) servers.add(ServerData.mcc$of(modern.get(i)));
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Client").warn("Could not read the server list: {}", e.toString());
        }
    }

    public void saveServerList() {
        try {
            var modern = modern();
            modern.load();
            while (modern.size() > 0) modern.remove(modern.get(0));
            for (ServerData s : servers) modern.add(s.mcc$modern(), false);
            modern.save();
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Client").warn("Could not save the server list: {}", e.toString());
        }
    }

    public ServerData getServerData(int index) {
        return servers.get(index);
    }

    public void removeServerData(int index) {
        servers.remove(index);
    }

    public void addServerData(ServerData server) {
        servers.add(server);
    }

    public int countServers() {
        return servers.size();
    }

    public void swapServers(int pos1, int pos2) {
        ServerData a = getServerData(pos1);
        servers.set(pos1, getServerData(pos2));
        servers.set(pos2, a);
        saveServerList();
    }

    public void set(int index, ServerData server) {
        servers.set(index, server);
    }

    /** Updates the saved entry with the same address (the server played on changed its resource pack choice). */
    public static void saveSingleServer(ServerData server) {
        if (server == null) return;
        try {
            net.minecraft.client.multiplayer.ServerList.saveSingleServer(server.mcc$modern());
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Client").warn("Could not save the server entry: {}", e.toString());
        }
    }
}
