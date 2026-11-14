package legacyconnector.vb1_7_3.net.minecraft.server;

/**
 * Beta 1.7.3 {@code MinecraftServer} as ModLoader(MP) mods reach it ({@code ModLoader.getMinecraftServerInstance()}). Mods call it from their
 * static initialisers and constructors, long before the modern server exists, so the one instance is always available: {@link #sendMessage}
 * goes to the server console and, when the modern server is running, to its players. The world and player-manager members are synthesized.
 */
public class MinecraftServer {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3");
    private static final MinecraftServer INSTANCE = new MinecraftServer();

    public static MinecraftServer mcc$instance() {
        return INSTANCE;
    }

    /** The original logged the message to the console; ModLoaderMP mods use it for their start-up notes. */
    public void sendMessage(String message) {
        if (message == null) return;
        LOG.info("[Server] {}", message);
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal(message), false);
    }
}
