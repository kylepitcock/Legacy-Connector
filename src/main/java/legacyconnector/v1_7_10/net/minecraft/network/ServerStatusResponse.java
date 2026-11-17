package legacyconnector.v1_7_10.net.minecraft.network;

import com.mojang.authlib.GameProfile;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;

/**
 * What the server list shows about the server (1.7.10 {@code ServerStatusResponse}). The description is the server's message of the day:
 * reading it gives the current MOTD and setting it changes the MOTD that the modern status ping reports. The player counts are the live
 * counts of the server; the counts a mod sets itself stay as it set them, and the sample of names is kept on the object (the modern ping builds its own sample).
 */
public class ServerStatusResponse {
    private final MinecraftServer mcc$server;
    private IChatComponent description;
    private PlayerCountData players;
    private MinecraftProtocolVersionIdentifier version = new MinecraftProtocolVersionIdentifier("1.7.10", 5);
    private String favicon;

    public ServerStatusResponse() {
        this(null);
    }

    public ServerStatusResponse(MinecraftServer server) {
        this.mcc$server = server;
    }

    public IChatComponent getServerDescription() {
        if (description != null) return description;
        return new ChatComponentText(mcc$server == null ? "" : mcc$server.getMOTD());
    }

    public void setServerDescription(IChatComponent description) {
        this.description = description;
        if (mcc$server != null && description != null) mcc$server.setMOTD(description.getFormattedText());
    }

    public PlayerCountData getPlayerCountData() {
        if (players == null) {
            players = new PlayerCountData(mcc$server == null ? 0 : mcc$server.getMaxPlayers(), mcc$server == null ? 0 : mcc$server.getCurrentPlayerCount());
            players.mcc$overridden = false;
        } else if (mcc$server != null) {
            players.mcc$refresh(mcc$server.getMaxPlayers(), mcc$server.getCurrentPlayerCount());
        }
        return players;
    }

    public void setPlayerCountData(PlayerCountData players) {
        this.players = players;
    }

    public MinecraftProtocolVersionIdentifier getProtocolVersionInfo() {
        return version;
    }

    public void setProtocolVersionInfo(MinecraftProtocolVersionIdentifier version) {
        this.version = version;
    }

    public void setFavicon(String favicon) {
        this.favicon = favicon;
    }

    public String getFavicon() {
        return favicon;
    }

    /** The player counts and the sample of names listed in the server list. */
    public static class PlayerCountData {
        private int maxPlayers;
        private int onlinePlayerCount;
        private GameProfile[] players;
        private boolean mcc$overridden;

        public PlayerCountData(int maxPlayers, int onlinePlayerCount) {
            this.maxPlayers = maxPlayers;
            this.onlinePlayerCount = onlinePlayerCount;
            this.mcc$overridden = true;
        }

        void mcc$refresh(int max, int online) {
            if (mcc$overridden) return;
            this.maxPlayers = max;
            this.onlinePlayerCount = online;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public int getOnlinePlayerCount() {
            return onlinePlayerCount;
        }

        public GameProfile[] getPlayers() {
            return players;
        }

        public void setPlayers(GameProfile[] players) {
            this.players = players;
        }
    }

    /** The version name and protocol number shown next to the server. */
    public static class MinecraftProtocolVersionIdentifier {
        private final String name;
        private final int protocol;

        public MinecraftProtocolVersionIdentifier(String name, int protocol) {
            this.name = name;
            this.protocol = protocol;
        }

        public String getName() {
            return name;
        }

        public int getProtocol() {
            return protocol;
        }
    }
}
