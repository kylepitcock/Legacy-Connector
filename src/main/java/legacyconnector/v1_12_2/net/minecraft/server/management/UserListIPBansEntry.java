package legacyconnector.v1_12_2.net.minecraft.server.management;

import com.google.gson.JsonObject;
import java.util.Date;

/** A banned IP address. */
public class UserListIPBansEntry extends UserListEntryBan<String> {
    public UserListIPBansEntry(String ip) {
        this(ip, null, null, null, null);
    }

    public UserListIPBansEntry(String ip, Date start, String bannedBy, Date end, String reason) {
        super(ip, start, bannedBy, end, reason);
    }

    public UserListIPBansEntry(JsonObject json) {
        super(json.has("ip") ? json.get("ip").getAsString() : null, json);
    }

    @Override
    protected void onSerialization(JsonObject json) {
        if (value != null) json.addProperty("ip", value);
        super.onSerialization(json);
    }

    /** The modern entry with the same data. */
    public net.minecraft.server.players.IpBanListEntry mcc$toModern() {
        return new net.minecraft.server.players.IpBanListEntry(value, mcc$start(), mcc$source(), getBanEndDate(), getBanReason());
    }

    static UserListIPBansEntry mcc$fromModern(String ip, net.minecraft.server.players.IpBanListEntry modern) {
        return new UserListIPBansEntry(ip, modern.getCreated(), modern.getSource(), modern.getExpires(), modern.getReason());
    }
}
