package legacyconnector.v1_12_2.net.minecraft.server.integrated;

/** The player list of the single player server (a mod's own list may extend it); the game keeps the real list. */
public class IntegratedPlayerList extends legacyconnector.v1_12_2.net.minecraft.server.management.PlayerList {
    public IntegratedPlayerList(IntegratedServer server) {
        super(server);
    }

    /** The data of the world's host player, or null before the world has one. */
    public legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound getHostPlayerData() {
        return null;
    }
}
