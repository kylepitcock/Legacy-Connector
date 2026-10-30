package legacyconnector.v1_12_2.net.minecraft.server.management;

import com.google.gson.JsonObject;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/** The banned IP addresses: the server's list is a view of the modern IP ban list. */
public class UserListIPBans extends UserList<String, UserListIPBansEntry> {
    private final net.minecraft.server.players.IpBanList mcc$modern;

    public UserListIPBans(File file) {
        super(file);
        this.mcc$modern = null;
    }

    /** The view of the server's modern IP ban list (an empty list without a server). */
    public UserListIPBans(net.minecraft.server.players.IpBanList modern) {
        super(modern == null ? null : modern.getFile());
        this.mcc$modern = modern;
    }

    @Override
    protected UserListEntry createEntry(JsonObject json) {
        return new UserListIPBansEntry(json);
    }

    public boolean isBanned(SocketAddress address) {
        String ip = addressToString(address);
        return mcc$modern != null ? mcc$modern.isBanned(ip) : hasEntry(ip);
    }

    public UserListIPBansEntry getBanEntry(SocketAddress address) {
        return getEntry(addressToString(address));
    }

    /** The address without port, as the list keys it ("127.0.0.1"). */
    private static String addressToString(SocketAddress address) {
        if (address instanceof InetSocketAddress inet && inet.getAddress() != null) return inet.getAddress().getHostAddress();
        String s = String.valueOf(address);
        if (s.startsWith("/")) s = s.substring(1);
        int colon = s.lastIndexOf(':');
        return colon > 0 ? s.substring(0, colon) : s;
    }

    @Override
    public String[] getKeys() {
        return mcc$modern != null ? mcc$modern.getUserList() : super.getKeys();
    }

    @Override
    public void addEntry(UserListIPBansEntry entry) {
        if (mcc$modern == null) {
            super.addEntry(entry);
            return;
        }
        mcc$modern.add(entry.mcc$toModern());
        // as the ban-ip command does: players on that address are disconnected
        var server = mcc$server();
        if (server != null && entry.getValue() != null) {
            for (var player : java.util.List.copyOf(server.getPlayerList().getPlayers())) {
                if (entry.getValue().equals(player.getIpAddress())) {
                    player.connection.disconnect(net.minecraft.network.chat.Component.translatable("multiplayer.disconnect.ip_banned"));
                }
            }
        }
    }

    @Override
    public UserListIPBansEntry getEntry(String ip) {
        if (mcc$modern == null) return super.getEntry(ip);
        var e = ip == null ? null : mcc$modern.get(ip);
        return e == null ? null : UserListIPBansEntry.mcc$fromModern(ip, e);
    }

    @Override
    public void removeEntry(String ip) {
        if (mcc$modern == null) super.removeEntry(ip);
        else mcc$modern.remove(ip);
    }

    @Override
    protected boolean hasEntry(String ip) {
        return mcc$modern == null ? super.hasEntry(ip) : mcc$modern.get(ip) != null;
    }

    @Override
    public void writeChanges() throws java.io.IOException {
        if (mcc$modern == null) super.writeChanges();
        else mcc$modern.save();
    }
}
