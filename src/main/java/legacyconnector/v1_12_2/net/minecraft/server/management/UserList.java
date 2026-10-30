package legacyconnector.v1_12_2.net.minecraft.server.management;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A JSON user list (ops, bans, whitelist), keyed by {@link #getObjectKey}. The server's own lists are the subclasses'
 * views of the modern lists; a list a mod creates itself keeps its entries here and saves them to its file.
 */
public class UserList<K, V extends UserListEntry<K>> {
    public static final Logger logger = LogManager.getLogger();
    final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    final File saveFile;
    final Map<String, V> values = new HashMap<>();
    boolean lanServer = true;

    public UserList(File saveFile) {
        this.saveFile = saveFile;
    }

    public boolean isLanServer() {
        return lanServer;
    }

    public void setLanServer(boolean lanServer) {
        this.lanServer = lanServer;
    }

    public void addEntry(V entry) {
        values.put(getObjectKey(entry.getValue()), entry);
        try {
            writeChanges();
        } catch (IOException e) {
            logger.warn("Could not save the list after adding a user.", e);
        }
    }

    public V getEntry(K key) {
        removeExpired();
        return values.get(getObjectKey(key));
    }

    public void removeEntry(K key) {
        values.remove(getObjectKey(key));
        try {
            writeChanges();
        } catch (IOException e) {
            logger.warn("Could not save the list after removing a user.", e);
        }
    }

    public String[] getKeys() {
        return values.keySet().toArray(new String[0]);
    }

    protected String getObjectKey(K key) {
        return key.toString();
    }

    protected boolean hasEntry(K key) {
        return values.containsKey(getObjectKey(key));
    }

    private void removeExpired() {
        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, V> e : values.entrySet()) if (e.getValue().hasBanExpired()) expired.add(e.getKey());
        for (String key : expired) values.remove(key);
    }

    protected UserListEntry createEntry(JsonObject json) {
        return new UserListEntry<>(null, json);
    }

    protected Map getValues() {
        return values;
    }

    /** Saves the entries to the list's file as a JSON array. */
    public void writeChanges() throws IOException {
        if (saveFile == null) return;
        JsonArray array = new JsonArray();
        for (V entry : values.values()) array.add(entry.mcc$serialize());
        com.google.common.io.Files.asCharSink(saveFile, StandardCharsets.UTF_8).write(gson.toJson(array));
    }

    /** Reads the entries back from the list's file. */
    @SuppressWarnings("unchecked")
    public void mcc$readSavedFile() throws IOException {
        if (saveFile == null || !saveFile.isFile()) return;
        JsonElement root = gson.fromJson(com.google.common.io.Files.asCharSource(saveFile, StandardCharsets.UTF_8).read(), JsonElement.class);
        if (root == null || !root.isJsonArray()) return;
        values.clear();
        for (JsonElement element : root.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            UserListEntry<K> entry = createEntry(element.getAsJsonObject());
            if (entry != null && entry.getValue() != null) values.put(getObjectKey(entry.getValue()), (V) entry);
        }
    }

    // ------------------------------------------------------------------ modern lists

    /** The running server, or null. */
    static net.minecraft.server.MinecraftServer mcc$server() {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    /** The profile of a name the modern list holds, via the server's profile cache (modern entries hide their profile). */
    static com.mojang.authlib.GameProfile mcc$profileIn(net.minecraft.server.players.StoredUserList<com.mojang.authlib.GameProfile, ?> list, String name) {
        if (list == null || name == null) return null;
        boolean listed = false;
        for (String n : list.getUserList()) {
            if (n.equalsIgnoreCase(name)) {
                listed = true;
                name = n;
                break;
            }
        }
        var server = mcc$server();
        if (!listed || server == null || server.getProfileCache() == null) return null;
        var profile = server.getProfileCache().get(name).orElse(null);
        return profile != null && list.get(profile) != null ? profile : null;
    }
}
