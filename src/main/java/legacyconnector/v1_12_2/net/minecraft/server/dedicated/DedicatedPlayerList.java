package legacyconnector.v1_12_2.net.minecraft.server.dedicated;

/** The player list of the dedicated server (a mod's own list may extend it); the game keeps the real list. */
public class DedicatedPlayerList extends legacyconnector.v1_12_2.net.minecraft.server.management.PlayerList {
    public DedicatedPlayerList(DedicatedServer server) {
        super(server);
    }
}
