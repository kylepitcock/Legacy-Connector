package legacyconnector.v1_12_2.net.minecraft.client.multiplayer;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/**
 * A server list entry (name, address, what its last ping answered). A plain data object as in 1.12; {@link #mcc$modern} and
 * {@link #mcc$of} convert to and from the game's own entries (servers.dat, the multiplayer screen, the server played on).
 */
public class ServerData {
    public String serverName;
    public String serverIP;
    public String populationInfo;
    public String serverMOTD;
    public long pingToServer;
    public int version = 340;
    public String gameVersion = "1.12.2";
    public boolean pinged;
    public String playerList;
    private ServerResourceMode resourceMode = ServerResourceMode.PROMPT;
    private String serverIcon;
    private boolean lanServer;

    public ServerData(String name, String ip, boolean isLan) {
        this.serverName = name;
        this.serverIP = ip;
        this.lanServer = isLan;
    }

    public NBTTagCompound getNBTCompound() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("name", serverName);
        tag.setString("ip", serverIP);
        if (serverIcon != null) tag.setString("icon", serverIcon);
        if (resourceMode == ServerResourceMode.ENABLED) tag.setBoolean("acceptTextures", true);
        else if (resourceMode == ServerResourceMode.DISABLED) tag.setBoolean("acceptTextures", false);
        return tag;
    }

    public ServerResourceMode getResourceMode() {
        return resourceMode;
    }

    public void setResourceMode(ServerResourceMode mode) {
        this.resourceMode = mode;
    }

    public static ServerData getServerDataFromNBTCompound(NBTTagCompound tag) {
        ServerData data = new ServerData(tag.getString("name"), tag.getString("ip"), false);
        if (tag.hasKey("icon", 8)) data.setBase64EncodedIconData(tag.getString("icon"));
        if (tag.hasKey("acceptTextures", 1)) {
            data.setResourceMode(tag.getBoolean("acceptTextures") ? ServerResourceMode.ENABLED : ServerResourceMode.DISABLED);
        } else {
            data.setResourceMode(ServerResourceMode.PROMPT);
        }
        return data;
    }

    public String getBase64EncodedIconData() {
        return serverIcon;
    }

    public void setBase64EncodedIconData(String icon) {
        this.serverIcon = icon;
    }

    public boolean isOnLAN() {
        return lanServer;
    }

    public void copyFrom(ServerData other) {
        this.serverIP = other.serverIP;
        this.serverName = other.serverName;
        this.setResourceMode(other.getResourceMode());
        this.serverIcon = other.serverIcon;
        this.lanServer = other.lanServer;
    }

    /** The game's entry for this one (name, address, icon, resource pack choice). */
    public net.minecraft.client.multiplayer.ServerData mcc$modern() {
        var out = new net.minecraft.client.multiplayer.ServerData(serverName == null ? "" : serverName, serverIP == null ? "" : serverIP,
                lanServer ? net.minecraft.client.multiplayer.ServerData.Type.LAN : net.minecraft.client.multiplayer.ServerData.Type.OTHER);
        out.setResourcePackStatus(switch (resourceMode == null ? ServerResourceMode.PROMPT : resourceMode) {
            case ENABLED -> net.minecraft.client.multiplayer.ServerData.ServerPackStatus.ENABLED;
            case DISABLED -> net.minecraft.client.multiplayer.ServerData.ServerPackStatus.DISABLED;
            default -> net.minecraft.client.multiplayer.ServerData.ServerPackStatus.PROMPT;
        });
        if (serverIcon != null) {
            try {
                out.setIconBytes(java.util.Base64.getDecoder().decode(serverIcon));
            } catch (IllegalArgumentException e) {
                // a broken icon is dropped, as 1.12 did
            }
        }
        return out;
    }

    /** A legacy entry with the game's entry's name, address, icon and last ping results. */
    public static ServerData mcc$of(net.minecraft.client.multiplayer.ServerData modern) {
        ServerData out = new ServerData(modern.name, modern.ip, modern.isLan());
        out.mcc$update(modern);
        return out;
    }

    /** Copies what the game knows about the server now (ping results come in asynchronously). */
    public void mcc$update(net.minecraft.client.multiplayer.ServerData modern) {
        serverName = modern.name;
        serverIP = modern.ip;
        serverMOTD = modern.motd == null ? null : org.kylepitcock.legacyconnector.v1_12_2.client.ClientGui1122.legacyText(modern.motd);
        populationInfo = modern.status == null ? null : org.kylepitcock.legacyconnector.v1_12_2.client.ClientGui1122.legacyText(modern.status);
        pingToServer = modern.ping;
        version = modern.protocol;
        gameVersion = modern.version == null ? gameVersion : modern.version.getString();
        if (modern.playerList != null && !modern.playerList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (var line : modern.playerList) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(line.getString());
            }
            playerList = sb.toString();
        }
        byte[] icon = modern.getIconBytes();
        serverIcon = icon == null ? null : java.util.Base64.getEncoder().encodeToString(icon);
        resourceMode = switch (modern.getResourcePackStatus()) {
            case ENABLED -> ServerResourceMode.ENABLED;
            case DISABLED -> ServerResourceMode.DISABLED;
            default -> ServerResourceMode.PROMPT;
        };
        if (modern.state() == net.minecraft.client.multiplayer.ServerData.State.SUCCESSFUL
                || modern.state() == net.minecraft.client.multiplayer.ServerData.State.INCOMPATIBLE
                || modern.state() == net.minecraft.client.multiplayer.ServerData.State.UNREACHABLE) pinged = true;
    }

    public enum ServerResourceMode {
        ENABLED("enabled"),
        DISABLED("disabled"),
        PROMPT("prompt");

        private final legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent motd;

        ServerResourceMode(String name) {
            this.motd = new legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation("addServer.resourcePack." + name);
        }

        public legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent getMotd() {
            return motd;
        }
    }
}
