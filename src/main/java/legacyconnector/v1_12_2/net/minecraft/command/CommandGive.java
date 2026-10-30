package legacyconnector.v1_12_2.net.minecraft.command;

import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;

/** {@code /give}: run as the modern command (mods mostly use its inherited {@code getItemByText}). */
public class CommandGive extends CommandBase {
    @Override
    public String getName() {
        return "give";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.give.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) throw new WrongUsageException("commands.give.usage");
        var m = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (m == null) return;
        String count = args.length > 2 ? " " + args[2] : "";
        m.getCommands().performPrefixedCommand(m.createCommandSourceStack(), "give " + args[0] + " " + args[1] + count);
    }
}
