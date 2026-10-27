package legacyconnector.v1_12_2.net.minecraft.server.management;

import com.mojang.authlib.GameProfile;

/** An operator: the profile, its permission level and whether it bypasses the player limit. */
public class UserListOpsEntry extends UserListEntry<GameProfile> {
    private final int permissionLevel;
    private final boolean bypassesPlayerLimit;

    public UserListOpsEntry(GameProfile player, int permissionLevelIn, boolean bypassesPlayerLimitIn) {
        super(player);
        this.permissionLevel = permissionLevelIn;
        this.bypassesPlayerLimit = bypassesPlayerLimitIn;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public boolean bypassesPlayerLimit() {
        return bypassesPlayerLimit;
    }
}
