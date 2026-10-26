package legacyconnector.v1_7_10.net.minecraft.server.management;

import com.mojang.authlib.GameProfile;
import java.util.UUID;

/** The server's username -> profile cache, backed by the modern profile cache. */
public class PlayerProfileCache {
    private final net.minecraft.server.MinecraftServer mcc$server;

    /** Username (lower case) -> entry; mods reflect it (CodeChickenLib's ServerUtils), the lookups here use the modern cache. */
    public java.util.Map<String, Object> field_152661_c = new java.util.LinkedHashMap<>();

    public PlayerProfileCache(net.minecraft.server.MinecraftServer server) {
        this.mcc$server = server;
    }

    /** Online players first: 1.7.10's cache always held them, the modern one misses offline-mode players (EnderIO's login). */
    public GameProfile getGameProfileForUsername(String name) {
        if (mcc$server == null || name == null) return null;
        var online = mcc$server.getPlayerList() == null ? null : mcc$server.getPlayerList().getPlayerByName(name);
        if (online != null) return online.getGameProfile();
        if (mcc$server.getProfileCache() == null) return null;
        return mcc$server.getProfileCache().get(name).orElse(null);
    }

    public GameProfile func_152652_a(UUID id) {
        if (mcc$server == null || id == null) return null;
        var online = mcc$server.getPlayerList() == null ? null : mcc$server.getPlayerList().getPlayer(id);
        if (online != null) return online.getGameProfile();
        if (mcc$server.getProfileCache() == null) return null;
        return mcc$server.getProfileCache().get(id).orElse(null);
    }

    public void func_152649_a(GameProfile profile) {
        if (mcc$server != null && mcc$server.getProfileCache() != null && profile != null) mcc$server.getProfileCache().add(profile);
    }

    public String[] func_152654_a() {
        return new String[0];
    }
}
