package legacyconnector.v1_7_10.net.minecraft.server.management;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;

/** A whitelisted player. */
public class UserListWhitelistEntry extends UserListEntry<GameProfile> {
    public UserListWhitelistEntry(GameProfile profile) {
        super(profile);
    }

    public UserListWhitelistEntry(JsonObject json) {
        super(mcc$readProfile(json), json);
    }

    @Override
    protected void onSerialization(JsonObject json) {
        mcc$writeProfile(json, value);
    }
}
