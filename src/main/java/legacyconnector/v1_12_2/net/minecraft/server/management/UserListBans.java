package legacyconnector.v1_12_2.net.minecraft.server.management;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.File;

/** The banned players: the server's list is a view of the modern ban list, keyed by UUID. */
public class UserListBans extends UserList<GameProfile, UserListBansEntry> {
    private final net.minecraft.server.players.UserBanList mcc$modern;

    public UserListBans(File file) {
        super(file);
        this.mcc$modern = null;
    }

    /** The view of the server's modern ban list. */
    public UserListBans(net.minecraft.server.players.UserBanList modern) {
        super(modern.getFile());
        this.mcc$modern = modern;
    }

    @Override
    protected UserListEntry createEntry(JsonObject json) {
        return new UserListBansEntry(json);
    }

    public boolean isBanned(GameProfile profile) {
        return mcc$modern != null ? mcc$modern.isBanned(profile) : hasEntry(profile);
    }

    @Override
    public String[] getKeys() {
        if (mcc$modern != null) return mcc$modern.getUserList();
        String[] names = new String[values.size()];
        int i = 0;
        for (UserListBansEntry e : values.values()) names[i++] = e.getValue().getName();
        return names;
    }

    @Override
    protected String getObjectKey(GameProfile profile) {
        return profile.getId().toString();
    }

    /** The profile of a banned name, or null. */
    public GameProfile getBannedProfile(String name) {
        if (mcc$modern != null) return mcc$profileIn(mcc$modern, name);
        for (UserListBansEntry e : values.values()) if (name.equalsIgnoreCase(e.getValue().getName())) return e.getValue();
        return null;
    }

    /** Adds the ban; on the server's list a banned player who is online is disconnected, as the ban command does. */
    @Override
    public void addEntry(UserListBansEntry entry) {
        if (mcc$modern == null) {
            super.addEntry(entry);
            return;
        }
        mcc$modern.add(entry.mcc$toModern());
        var server = mcc$server();
        if (server != null && entry.getValue() != null) {
            var player = server.getPlayerList().getPlayer(entry.getValue().getId());
            if (player != null) player.connection.disconnect(net.minecraft.network.chat.Component.translatable("multiplayer.disconnect.banned"));
        }
    }

    @Override
    public UserListBansEntry getEntry(GameProfile profile) {
        if (mcc$modern == null) return super.getEntry(profile);
        var e = mcc$modern.get(profile);
        return e == null ? null : UserListBansEntry.mcc$fromModern(profile, e);
    }

    @Override
    public void removeEntry(GameProfile profile) {
        if (mcc$modern == null) super.removeEntry(profile);
        else mcc$modern.remove(profile);
    }

    @Override
    protected boolean hasEntry(GameProfile profile) {
        return mcc$modern == null ? super.hasEntry(profile) : mcc$modern.get(profile) != null;
    }

    @Override
    public void writeChanges() throws java.io.IOException {
        if (mcc$modern == null) super.writeChanges();
        else mcc$modern.save();
    }
}
