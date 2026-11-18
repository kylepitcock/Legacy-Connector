package legacyconnector.v1_7_10.net.minecraft.client.network;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.client.multiplayer.ServerData;

/**
 * 1.7.10's multiplayer-list pinger: {@code func_147224_a} starts a status ping of a server, {@code func_147223_a} (once a tick) lets the
 * connections progress, and the entry's MOTD, player counts, version and ping fill in as the replies arrive. It runs the modern game's pinger on
 * a modern copy of the entry and copies its result into the legacy {@link ServerData}.
 */
public class OldServerPinger {
    private final net.minecraft.client.multiplayer.ServerStatusPinger pinger = new net.minecraft.client.multiplayer.ServerStatusPinger();
    private final List<Object[]> pending = new ArrayList<>();

    public OldServerPinger() {
    }

    public void func_147224_a(ServerData data) throws UnknownHostException {
        var modern = new net.minecraft.client.multiplayer.ServerData(String.valueOf(data.serverName), String.valueOf(data.serverIP),
                net.minecraft.client.multiplayer.ServerData.Type.OTHER);
        synchronized (pending) {
            pending.add(new Object[] {data, modern});
        }
        try {
            pinger.pingServer(modern, () -> {
            }, () -> {
            });
        } catch (Throwable t) {
            // dead or unresolvable host: the vanilla list shows "Can't connect to server" instead of failing the screen
            synchronized (pending) {
                pending.removeIf(e -> e[0] == data);
            }
            data.serverMOTD = "Can't connect to server";
            data.populationInfo = "";
            data.pingToServer = -1L;
        }
    }

    /** Lets the pending connections progress and copies the results of the finished pings. */
    public void func_147223_a() {
        pinger.tick();
        synchronized (pending) {
            for (Iterator<Object[]> it = pending.iterator(); it.hasNext(); ) {
                Object[] entry = it.next();
                ServerData data = (ServerData) entry[0];
                var modern = (net.minecraft.client.multiplayer.ServerData) entry[1];
                switch (modern.state()) {
                    case SUCCESSFUL, INCOMPATIBLE -> {
                        data.serverMOTD = modern.motd.getString();
                        data.populationInfo = modern.status.getString();
                        data.gameVersion = modern.version.getString();
                        data.pingToServer = modern.ping;
                        if (modern.getIconBytes() != null) data.setBase64EncodedIconData(java.util.Base64.getEncoder().encodeToString(modern.getIconBytes()));
                        if (modern.ping >= 0 || modern.state() == net.minecraft.client.multiplayer.ServerData.State.INCOMPATIBLE) it.remove();
                    }
                    case UNREACHABLE -> {
                        data.serverMOTD = "Can't connect to server";
                        data.populationInfo = "";
                        data.pingToServer = -1L;
                        it.remove();
                    }
                    default -> {
                    }
                }
            }
        }
    }

    /** Closes the connections still open. */
    public void func_147226_b() {
        pinger.removeAll();
        synchronized (pending) {
            pending.clear();
        }
    }
}
