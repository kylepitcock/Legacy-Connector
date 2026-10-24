package legacyconnector.v1_7_10.net.minecraft.server.integrated;

import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;

/** The single-player server (1.7.10 clients reach it through {@code Minecraft.getIntegratedServer()}). */
public class IntegratedServer extends MinecraftServer {
    public IntegratedServer(net.minecraft.server.MinecraftServer server) {
        super(server);
    }

    public boolean getPublic() {
        return mcc$modern().isPublished();
    }

    public boolean isServerRunning() {
        return mcc$modern().isRunning();
    }
}
