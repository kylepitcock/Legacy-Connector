package legacyconnector.v1_12_2.net.minecraft.server.integrated;

/**
 * The single player server ({@code Minecraft.getIntegratedServer()}): like {@code MinecraftServer} a view answering from the modern server
 * that runs. On a client it is the {@code MinecraftServer} view itself.
 */
public class IntegratedServer extends legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer {
    public IntegratedServer() {
    }

    private static net.minecraft.server.MinecraftServer mcc$server() {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    /** Whether the world is open to LAN. */
    public boolean getPublic() {
        var m = mcc$server();
        return m != null && m.isPublished();
    }

    /** The world's display name (the folder is {@code getFolderName}). */
    public String getWorldName() {
        var m = mcc$server();
        return m == null ? "" : m.getWorldData().getLevelName();
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }
}
