package legacyconnector.v1_12_2.net.minecraft.server.management;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;

/** The server's name/UUID cache (usercache.json): a view over the modern cache. */
public class PlayerProfileCache {
    /** Phantom field: accesses are redirected to {@link #mcc$get$usernameToProfileEntryMap}. */
    @Deprecated
    public Map<String, ProfileEntry> usernameToProfileEntryMap;

    public PlayerProfileCache() {
    }

    private static net.minecraft.server.players.GameProfileCache modern() {
        var s = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return s == null ? null : s.getProfileCache();
    }

    /** The profiles of the online players by lower-cased name (the modern cache does not list its entries). */
    public static Map<String, ProfileEntry> mcc$get$usernameToProfileEntryMap(PlayerProfileCache cache) {
        Map<String, ProfileEntry> out = new java.util.HashMap<>();
        var s = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (s != null) {
            for (var p : s.getPlayerList().getPlayers()) {
                out.put(p.getGameProfile().getName().toLowerCase(java.util.Locale.ROOT), cache.new ProfileEntry(p.getGameProfile(), null));
            }
        }
        return out;
    }

    public GameProfile getGameProfileForUsername(String username) {
        var m = modern();
        return m == null ? null : m.get(username).orElse(null);
    }

    public GameProfile getProfileByUUID(UUID uuid) {
        var m = modern();
        return m == null ? null : m.get(uuid).orElse(null);
    }

    public void addEntry(GameProfile gameProfile) {
        var m = modern();
        if (m != null && gameProfile != null) m.add(gameProfile);
    }

    public String[] getUsernames() {
        return mcc$get$usernameToProfileEntryMap(this).keySet().toArray(new String[0]);
    }

    public void save() {
        var m = modern();
        if (m != null) m.save();
    }

    public static boolean isOnlineMode() {
        var s = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return s != null && s.usesAuthentication();
    }

    public static void setOnlineMode(boolean onlineMode) {
    }

    public class ProfileEntry {
        private final GameProfile gameProfile;
        private final java.util.Date expirationDate;

        ProfileEntry(GameProfile gameProfileIn, java.util.Date expirationDateIn) {
            this.gameProfile = gameProfileIn;
            this.expirationDate = expirationDateIn;
        }

        public GameProfile getGameProfile() {
            return gameProfile;
        }

        public java.util.Date getExpirationDate() {
            return expirationDate;
        }
    }
}
