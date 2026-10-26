package legacyconnector.v1_12_2.net.minecraft.server;

import legacyconnector.v1_12_2.net.minecraft.command.CommandHandler;
import legacyconnector.v1_12_2.net.minecraft.command.ICommandManager;

/** The running server as 1.12 mods see it: what exists here is what needs no 1.12 world or player classes. */
public class MinecraftServer implements legacyconnector.v1_12_2.net.minecraft.command.ICommandSender {
    private static final MinecraftServer INSTANCE = new MinecraftServer();
    private final CommandHandler commandManager = new CommandHandler();

    public static MinecraftServer mcc$instance() {
        return INSTANCE;
    }

    public ICommandManager getCommandManager() {
        return commandManager;
    }

    // the server as a command sender (the console)

    @Override
    public String getName() {
        return "Server";
    }

    @Override
    public void sendMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent component) {
        if (component != null) org.slf4j.LoggerFactory.getLogger("LegacyConnector/1.12.2").info(component.getUnformattedText());
    }

    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        return true;
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.world.World getEntityWorld() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(server.overworld());
    }

    @Override
    public MinecraftServer getServer() {
        return this;
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
