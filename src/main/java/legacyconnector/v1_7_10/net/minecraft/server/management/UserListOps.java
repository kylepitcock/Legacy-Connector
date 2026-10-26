package legacyconnector.v1_7_10.net.minecraft.server.management;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.File;

/** The operators: the server's list is a view of the modern ops list, keyed by UUID; {@link #getKeys} gives the names. */
public class UserListOps extends UserList<GameProfile, UserListOpsEntry> {
    private final net.minecraft.server.players.ServerOpList mcc$modern;

    public UserListOps(File file) {
        super(file);
        this.mcc$modern = null;
    }

    /** The view of the server's modern ops list. */
    public UserListOps(net.minecraft.server.players.ServerOpList modern) {
        super(modern.getFile());
        this.mcc$modern = modern;
    }

    @Override
    protected UserListEntry createEntry(JsonObject json) {
        return new UserListOpsEntry(json);
    }

    @Override
    public String[] getKeys() {
        if (mcc$modern != null) return mcc$modern.getUserList();
        String[] names = new String[values.size()];
        int i = 0;
        for (UserListOpsEntry e : values.values()) names[i++] = e.getValue().getName();
        return names;
    }

    /** The key of a profile: its UUID. */
    protected String func_152699_b(GameProfile profile) {
        return profile.getId().toString();
    }

    @Override
    protected String getObjectKey(GameProfile profile) {
        return func_152699_b(profile);
    }

    public GameProfile getGameProfileFromName(String name) {
        if (mcc$modern != null) return mcc$profileIn(mcc$modern, name);
        for (UserListOpsEntry e : values.values()) if (name.equalsIgnoreCase(e.getValue().getName())) return e.getValue();
        return null;
    }

    @Override
    public void addEntry(UserListOpsEntry entry) {
        if (mcc$modern == null) {
            super.addEntry(entry);
            return;
        }
        mcc$modern.add(new net.minecraft.server.players.ServerOpListEntry(entry.getValue(), entry.func_152644_a(),
                mcc$modern.canBypassPlayerLimit(entry.getValue())));
        mcc$refresh(entry.getValue());
    }

    @Override
    public UserListOpsEntry getEntry(GameProfile profile) {
        if (mcc$modern == null) return super.getEntry(profile);
        var e = mcc$modern.get(profile);
        return e == null ? null : new UserListOpsEntry(profile, e.getLevel());
    }

    @Override
    public void removeEntry(GameProfile profile) {
        if (mcc$modern == null) {
            super.removeEntry(profile);
            return;
        }
        mcc$modern.remove(profile);
        mcc$refresh(profile);
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

    /** An online player whose op status changed gets their new command permissions sent. */
    private static void mcc$refresh(GameProfile profile) {
        var server = mcc$server();
        if (server == null || profile == null) return;
        var player = server.getPlayerList().getPlayer(profile.getId());
        if (player != null) server.getPlayerList().sendPlayerPermissionLevel(player);
    }
}
