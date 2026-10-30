package legacyconnector.v1_12_2.net.minecraft.server.management;

import com.google.gson.JsonObject;
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

    public UserListOpsEntry(JsonObject json) {
        super(mcc$readProfile(json), json);
        this.permissionLevel = json.has("level") ? json.get("level").getAsInt() : 0;
        this.bypassesPlayerLimit = json.has("bypassesPlayerLimit") && json.get("bypassesPlayerLimit").getAsBoolean();
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public boolean bypassesPlayerLimit() {
        return bypassesPlayerLimit;
    }

    @Override
    protected void onSerialization(JsonObject json) {
        mcc$writeProfile(json, value);
        json.addProperty("level", permissionLevel);
        json.addProperty("bypassesPlayerLimit", bypassesPlayerLimit);
    }
}
