package legacyconnector.v1_12_2.net.minecraft.client.network;

import java.net.UnknownHostException;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.client.multiplayer.ServerData;

/**
 * Asks servers for their status (MOTD, player count, version, latency) through the game's pinger; the answer is copied into the legacy
 * entry when it arrives ({@link ServerData#pinged} becomes true).
 */
public class ServerPinger {
    private final net.minecraft.client.multiplayer.ServerStatusPinger modern = new net.minecraft.client.multiplayer.ServerStatusPinger();
    private final Map<ServerData, net.minecraft.client.multiplayer.ServerData> pending = new java.util.concurrent.ConcurrentHashMap<>();

    public ServerPinger() {
    }

    public void ping(ServerData server) throws UnknownHostException {
        var data = server.mcc$modern();
        pending.put(server, data);
        Runnable update = () -> server.mcc$update(data);
        modern.pingServer(data, update, () -> {
            data.setState(data.protocol == net.minecraft.SharedConstants.getCurrentVersion().getProtocolVersion()
                    ? net.minecraft.client.multiplayer.ServerData.State.SUCCESSFUL : net.minecraft.client.multiplayer.ServerData.State.INCOMPATIBLE);
            server.mcc$update(data);
        });
    }

    /** Drives the pending pings (called every tick by the screen that pings) and copies what arrived. */
    public void pingPendingNetworks() {
        modern.tick();
        pending.forEach((legacy, data) -> legacy.mcc$update(data));
        pending.keySet().removeIf(l -> l.pinged);
    }

    public void clearPendingNetworks() {
        modern.removeAll();
        pending.clear();
    }
}
