package legacyconnector.v1_7_10.net.minecraft.command;

/** The /help command. The modern game provides its own; this exists for mods that subclass or name it. */
public class CommandHelp extends CommandBase {
    @Override
    public String getCommandName() {
        return "help";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.help.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
    }

    /** The commands the server's command manager knows, by name (1.7.10 listed them from here). */
    protected java.util.Map getCommands() {
        legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer server = legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer.getServer();
        ICommandManager manager = server == null ? null : server.getCommandManager();
        return manager == null ? new java.util.HashMap() : manager.getCommands();
    }
}
