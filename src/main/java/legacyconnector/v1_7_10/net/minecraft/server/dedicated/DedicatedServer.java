package legacyconnector.v1_7_10.net.minecraft.server.dedicated;

import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;

/**
 * The dedicated server, which in 1.7.10 is also the console's command sender: mods tell console commands from player ones with
 * {@code sender instanceof DedicatedServer} (MineTweaker's {@code /minetweaker reload} lets the console run it only so).
 */
public class DedicatedServer extends MinecraftServer {
    public DedicatedServer(net.minecraft.server.MinecraftServer server) {
        super(server);
    }

    public boolean isDedicatedServer() {
        return true;
    }
}
