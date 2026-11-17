package legacyconnector.v1_7_10.net.minecraft.server.integrated;

import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_7_10.net.minecraft.world.WorldSettings;

/** The single-player server (1.7.10 clients reach it through {@code Minecraft.getIntegratedServer()}). */
public class IntegratedServer extends MinecraftServer {
    public IntegratedServer(net.minecraft.server.MinecraftServer server) {
        super(server);
    }

    public void setOnlineMode(boolean online) {
        mcc$modern().setUsesAuthentication(online);
    }

    public boolean getPublic() {
        return mcc$modern().isPublished();
    }

    public boolean isServerRunning() {
        return mcc$modern().isRunning();
    }

    /** Opens the world to the local network; the port as text, or null when it could not be opened. */
    public String shareToLAN(WorldSettings.GameType type, boolean allowCheats) {
        try {
            if (!(mcc$modern() instanceof net.minecraft.client.server.IntegratedServer host)) return null;
            int port = net.minecraft.util.HttpUtil.getAvailablePort();
            net.minecraft.world.level.GameType mode = type == null || type.getID() < 0 ? host.getDefaultGameType()
                    : net.minecraft.world.level.GameType.byId(type.getID());
            return host.publishServer(mode, allowCheats, port) ? String.valueOf(port) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
