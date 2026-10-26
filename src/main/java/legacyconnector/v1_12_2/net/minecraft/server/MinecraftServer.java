package legacyconnector.v1_12_2.net.minecraft.server;

import legacyconnector.v1_12_2.net.minecraft.command.CommandHandler;
import legacyconnector.v1_12_2.net.minecraft.command.ICommandManager;

/** The running server as 1.12 mods see it: what exists here is what needs no 1.12 world or player classes. */
public class MinecraftServer {
    private static final MinecraftServer INSTANCE = new MinecraftServer();
    private final CommandHandler commandManager = new CommandHandler();

    public static MinecraftServer mcc$instance() {
        return INSTANCE;
    }

    public ICommandManager getCommandManager() {
        return commandManager;
    }

    public boolean isDedicatedServer() {
        return !net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
    }

    public boolean isSinglePlayer() {
        return net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
    }

    public String getMinecraftVersion() {
        return "1.12.2";
    }

    /** The game's directory (the server directory; the client's game directory in single player). */
    public java.io.File getDataDirectory() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().toFile() : server.getServerDirectory().toFile();
    }
}
