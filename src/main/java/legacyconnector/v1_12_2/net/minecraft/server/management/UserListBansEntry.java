package legacyconnector.v1_12_2.net.minecraft.server.management;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.util.Date;

/** A banned player. */
public class UserListBansEntry extends UserListEntryBan<GameProfile> {
    public UserListBansEntry(GameProfile profile) {
        this(profile, null, null, null, null);
    }

    public UserListBansEntry(GameProfile profile, Date start, String bannedBy, Date end, String reason) {
        super(profile, start, bannedBy, end, reason);
    }

    public UserListBansEntry(JsonObject json) {
        super(mcc$readProfile(json), json);
    }

    @Override
    protected void onSerialization(JsonObject json) {
        mcc$writeProfile(json, value);
        super.onSerialization(json);
    }

    /** The modern entry with the same data. */
    public net.minecraft.server.players.UserBanListEntry mcc$toModern() {
        return new net.minecraft.server.players.UserBanListEntry(value, banStartDate, bannedBy, banEndDate, reason);
    }

    static UserListBansEntry mcc$fromModern(GameProfile profile, net.minecraft.server.players.UserBanListEntry modern) {
        return new UserListBansEntry(profile, modern.getCreated(), modern.getSource(), modern.getExpires(), modern.getReason());
    }
}
