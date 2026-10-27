package legacyconnector.v1_12_2.net.minecraft.server.management;

import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;

/** The 1.7 name-to-UUID converter; only the owner lookup tamed mobs still use is left. */
public class PreYggdrasilConverter {
    private PreYggdrasilConverter() {}

    /** The UUID string of a pet's owner, which old saves stored by name. */
    public static String convertMobOwnerIfNeeded(MinecraftServer server, String username) {
        if (username == null || username.isEmpty() || username.length() > 16) return username;
        var s = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (s == null) return username;
        var profile = s.getProfileCache() == null ? null : s.getProfileCache().get(username).orElse(null);
        if (profile != null && profile.getId() != null) return profile.getId().toString();
        if (!s.usesAuthentication()) return net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(username).toString();
        return "";
    }
}
