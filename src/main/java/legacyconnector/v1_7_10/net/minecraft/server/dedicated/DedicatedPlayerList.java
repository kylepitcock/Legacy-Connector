package legacyconnector.v1_7_10.net.minecraft.server.dedicated;

import legacyconnector.v1_7_10.net.minecraft.server.management.ServerConfigurationManager;

/** The dedicated server's player list (the ops, bans and whitelist come from the host's files). */
public class DedicatedPlayerList extends ServerConfigurationManager {
    public DedicatedPlayerList(DedicatedServer server) {
        super(server);
    }
}
