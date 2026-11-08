package legacyconnector.v1_7_10.net.minecraft.client.multiplayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

/**
 * The multiplayer server list of 1.7.10 ({@code ServerList(Minecraft)} reads {@code servers.dat} of the game directory): the entries mods such as
 * Rich Presence mods read to name the server the player is on. Saving writes the same file back, so the list stays the game's own.
 */
public class ServerList {
    private final Path file;
    private final List<ServerData> servers = new ArrayList<>();

    public ServerList(legacyconnector.v1_7_10.net.minecraft.client.Minecraft mc) {
        Path dir;
        try {
            dir = net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath();
        } catch (RuntimeException | LinkageError e) {
            dir = Path.of(".");
        }
        this.file = dir.resolve("servers.dat");
        loadServerList();
    }

    /** Reads {@code servers.dat} (an absent or unreadable file leaves the list empty). */
    public void loadServerList() {
        servers.clear();
        if (!Files.isRegularFile(file)) return;
        try {
            CompoundTag root = NbtIo.read(file);
            if (root == null) return;
            ListTag list = root.getList("servers", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                ServerData data = new ServerData(tag.getString("name"), tag.getString("ip"));
                servers.add(data);
            }
        } catch (IOException | RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector").warn("servers.dat could not be read: {}", e.toString());
        }
    }

    /** Writes the list to {@code servers.dat}. */
    public void saveServerList() {
        try {
            ListTag list = new ListTag();
            for (ServerData data : servers) {
                CompoundTag tag = new CompoundTag();
                tag.putString("name", data.serverName == null ? "" : data.serverName);
                tag.putString("ip", data.serverIP == null ? "" : data.serverIP);
                list.add(tag);
            }
            CompoundTag root = new CompoundTag();
            root.put("servers", list);
            NbtIo.write(root, file);
        } catch (IOException | RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector").warn("servers.dat could not be written: {}", e.toString());
        }
    }

    public ServerData getServerData(int index) {
        return servers.get(index);
    }

    public void removeServerData(int index) {
        servers.remove(index);
    }

    public void addServerData(ServerData data) {
        servers.add(data);
    }

    public int countServers() {
        return servers.size();
    }

    public void swapServers(int a, int b) {
        ServerData tmp = servers.get(a);
        servers.set(a, servers.get(b));
        servers.set(b, tmp);
    }

    public void func_147413_a(int index, ServerData data) {
        servers.set(index, data);
    }
}
