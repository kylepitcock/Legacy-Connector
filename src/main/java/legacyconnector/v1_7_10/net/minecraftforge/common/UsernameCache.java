package legacyconnector.v1_7_10.net.minecraftforge.common;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Forge's UUID -> last known username cache, kept in memory (the modern game has its own profile cache). */
public class UsernameCache {
    private static final Map<UUID, String> MAP = new ConcurrentHashMap<>();

    private UsernameCache() {
    }

    public static void setUsername(UUID uuid, String username) {
        if (uuid != null && username != null) MAP.put(uuid, username);
    }

    public static boolean removeUsername(UUID uuid) {
        return uuid != null && MAP.remove(uuid) != null;
    }

    public static String getLastKnownUsername(UUID uuid) {
        return uuid == null ? null : MAP.get(uuid);
    }

    public static boolean containsUUID(UUID uuid) {
        return uuid != null && MAP.containsKey(uuid);
    }

    public static Map<UUID, String> getMap() {
        return Collections.unmodifiableMap(MAP);
    }
}
