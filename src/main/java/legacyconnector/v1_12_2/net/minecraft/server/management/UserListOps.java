package legacyconnector.v1_12_2.net.minecraft.server.management;

import com.mojang.authlib.GameProfile;

/** The server's operators: a view over the modern op list. */
public class UserListOps {
    private final net.minecraft.server.players.ServerOpList modern;

    public UserListOps(net.minecraft.server.players.ServerOpList modern) {
        this.modern = modern;
    }

    /** The entry of a profile (or null when it is no operator). */
    public UserListOpsEntry getEntry(Object obj) {
        if (modern == null || !(obj instanceof GameProfile profile)) return null;
        var e = modern.get(profile);
        return e == null ? null : new UserListOpsEntry(profile, e.getLevel(), e.getBypassesPlayerLimit());
    }

    public int getPermissionLevel(GameProfile profile) {
        UserListOpsEntry e = getEntry(profile);
        return e == null ? 0 : e.getPermissionLevel();
    }

    public boolean bypassesPlayerLimit(GameProfile profile) {
        UserListOpsEntry e = getEntry(profile);
        return e != null && e.bypassesPlayerLimit();
    }

    public String[] getKeys() {
        return modern == null ? new String[0] : modern.getUserList();
    }

    public GameProfile getGameProfileFromName(String username) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (modern == null || server == null || server.getProfileCache() == null) return null;
        GameProfile profile = server.getProfileCache().get(username).orElse(null);
        return profile != null && modern.get(profile) != null ? profile : null;
    }
}
