package legacyconnector.v1_12_2.net.minecraftforge.common;

import java.util.Map;
import java.util.UUID;

/** The last known name of each player id: the server's profile cache (and the online players). */
public final class UsernameCache {
    private UsernameCache() {}

    private static net.minecraft.server.MinecraftServer server() {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    public static String getLastKnownUsername(UUID uuid) {
        if (uuid == null) return null;
        var server = server();
        if (server == null) return null;
        var online = server.getPlayerList().getPlayer(uuid);
        if (online != null) return online.getGameProfile().getName();
        var cache = server.getProfileCache();
        return cache == null ? null : cache.get(uuid).map(com.mojang.authlib.GameProfile::getName).orElse(null);
    }

    public static boolean containsUUID(UUID uuid) {
        return getLastKnownUsername(uuid) != null;
    }

    public static Map<UUID, String> getMap() {
        Map<UUID, String> out = new java.util.HashMap<>();
        var server = server();
        if (server != null) for (var p : server.getPlayerList().getPlayers()) out.put(p.getUUID(), p.getGameProfile().getName());
        return java.util.Collections.unmodifiableMap(out);
    }
}
