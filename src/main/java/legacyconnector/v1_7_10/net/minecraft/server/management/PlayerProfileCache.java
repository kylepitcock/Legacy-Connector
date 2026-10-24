package legacyconnector.v1_7_10.net.minecraft.server.management;

import com.mojang.authlib.GameProfile;
import java.util.UUID;

/** The server's username -> profile cache, backed by the modern profile cache. */
public class PlayerProfileCache {
    private final net.minecraft.server.MinecraftServer mcc$server;

    public PlayerProfileCache(net.minecraft.server.MinecraftServer server) {
        this.mcc$server = server;
    }

    public GameProfile getGameProfileForUsername(String name) {
        if (mcc$server == null || mcc$server.getProfileCache() == null || name == null) return null;
        return mcc$server.getProfileCache().get(name).orElse(null);
    }

    public GameProfile func_152652_a(UUID id) {
        if (mcc$server == null || mcc$server.getProfileCache() == null || id == null) return null;
        return mcc$server.getProfileCache().get(id).orElse(null);
    }

    public void func_152649_a(GameProfile profile) {
        if (mcc$server != null && mcc$server.getProfileCache() != null && profile != null) mcc$server.getProfileCache().add(profile);
    }

    public String[] func_152654_a() {
        return new String[0];
    }
}
