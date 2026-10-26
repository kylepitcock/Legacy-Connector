package legacyconnector.v1_7_10.net.minecraft.server.management;

import java.io.File;

/** 1.7.10's converter of name-based server files to UUIDs; mods use its name -> UUID lookup for owners saved by name. */
public class PreYggdrasilConverter {
    public static final File OLD_IPBAN_FILE = new File("banned-ips.txt");
    public static final File OLD_PLAYERBAN_FILE = new File("banned-players.txt");
    public static final File OLD_OPS_FILE = new File("ops.txt");
    public static final File OLD_WHITELIST_FILE = new File("white-list.txt");

    /**
     * The UUID (as a string) of a player name: from the server's profile cache, else looked up at Mojang on an online-mode
     * server ("" when that fails), else the offline UUID. Strings that cannot be a name (empty, longer than 16) come back
     * unchanged: owners saved as UUIDs pass through.
     */
    public static String func_152719_a(String name) {
        if (name == null || name.isEmpty() || name.length() > 16) return name;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getProfileCache() == null) return net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(name).toString();
        var profile = server.getProfileCache().get(name).orElse(null);
        if (profile != null && profile.getId() != null) return profile.getId().toString();
        if (!server.isSingleplayer() && server.usesAuthentication()) return "";
        return net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(name).toString();
    }
}
