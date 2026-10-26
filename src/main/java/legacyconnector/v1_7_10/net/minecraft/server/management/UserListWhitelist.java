package legacyconnector.v1_7_10.net.minecraft.server.management;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.File;

/** The whitelisted players: the server's list is a view of the modern whitelist, keyed by UUID. */
public class UserListWhitelist extends UserList<GameProfile, UserListWhitelistEntry> {
    private final net.minecraft.server.players.UserWhiteList mcc$modern;

    public UserListWhitelist(File file) {
        super(file);
        this.mcc$modern = null;
    }

    /** The view of the server's modern whitelist. */
    public UserListWhitelist(net.minecraft.server.players.UserWhiteList modern) {
        super(modern.getFile());
        this.mcc$modern = modern;
    }

    @Override
    protected UserListEntry createEntry(JsonObject json) {
        return new UserListWhitelistEntry(json);
    }

    @Override
    public String[] getKeys() {
        if (mcc$modern != null) return mcc$modern.getUserList();
        String[] names = new String[values.size()];
        int i = 0;
        for (UserListWhitelistEntry e : values.values()) names[i++] = e.getValue().getName();
        return names;
    }

    protected String func_152704_b(GameProfile profile) {
        return profile.getId().toString();
    }

    @Override
    protected String getObjectKey(GameProfile profile) {
        return func_152704_b(profile);
    }

    /** The profile of a whitelisted name, or null. */
    public GameProfile func_152706_a(String name) {
        if (mcc$modern != null) return mcc$profileIn(mcc$modern, name);
        for (UserListWhitelistEntry e : values.values()) if (name.equalsIgnoreCase(e.getValue().getName())) return e.getValue();
        return null;
    }

    @Override
    public void addEntry(UserListWhitelistEntry entry) {
        if (mcc$modern == null) super.addEntry(entry);
        else mcc$modern.add(new net.minecraft.server.players.UserWhiteListEntry(entry.getValue()));
    }

    @Override
    public UserListWhitelistEntry getEntry(GameProfile profile) {
        if (mcc$modern == null) return super.getEntry(profile);
        return mcc$modern.get(profile) == null ? null : new UserListWhitelistEntry(profile);
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
