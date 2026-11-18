package legacyconnector.v1_12_2.net.minecraft.command;

import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;

/** {@code /help}: the modern command lists the commands (mods mostly subclass it to change the page they show). */
public class CommandHelp extends CommandBase {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.help.usage";
    }

    @Override
    public java.util.List<String> getAliases() {
        return java.util.Collections.singletonList("?");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        var m = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (m == null) return;
        m.getCommands().performPrefixedCommand(m.createCommandSourceStack(), args.length == 0 ? "help" : "help " + String.join(" ", args));
    }
}
