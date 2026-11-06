package legacyconnector.v1_7_10.net.minecraftforge.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge's UUID -> last known username cache. Forge 1.7.10 filled it when a player joined, before the login event (Ender IO's
 * hypercubes resolve a player name through it): the bridge fills it the same way, and the modern game's own cache (which the
 * game fills on join) is read too.
 */
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
        if (uuid == null) return null;
        String name = MAP.get(uuid);
        return name != null ? name : net.neoforged.neoforge.common.UsernameCache.getLastKnownUsername(uuid);
    }

    public static boolean containsUUID(UUID uuid) {
        return uuid != null && (MAP.containsKey(uuid) || net.neoforged.neoforge.common.UsernameCache.containsUUID(uuid));
    }

    public static Map<UUID, String> getMap() {
        Map<UUID, String> all = new HashMap<>(net.neoforged.neoforge.common.UsernameCache.getMap());
        all.putAll(MAP);
        return Collections.unmodifiableMap(all);
    }
}
