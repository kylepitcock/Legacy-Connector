package legacyconnector.v1_7_10.net.minecraft.server.management;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;

/** An operator and their permission level. */
public class UserListOpsEntry extends UserListEntry<GameProfile> {
    private final int field_152645_a;

    public UserListOpsEntry(GameProfile profile, int permissionLevel) {
        super(profile);
        this.field_152645_a = permissionLevel;
    }

    public UserListOpsEntry(JsonObject json) {
        super(mcc$readProfile(json), json);
        this.field_152645_a = json.has("level") ? json.get("level").getAsInt() : 0;
    }

    /** The permission level. */
    public int func_152644_a() {
        return field_152645_a;
    }

    @Override
    protected void onSerialization(JsonObject json) {
        mcc$writeProfile(json, value);
        json.addProperty("level", field_152645_a);
    }
}
