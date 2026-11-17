package legacyconnector.v1_12_2.net.minecraft.command.server;

import legacyconnector.v1_12_2.net.minecraft.command.CommandBase;
import legacyconnector.v1_12_2.net.minecraft.command.CommandException;
import legacyconnector.v1_12_2.net.minecraft.command.ICommandSender;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;

/**
 * Base of the 1.12.2 server commands the modern game already implements ({@code ban}, {@code op}, {@code whitelist}...): the line is run
 * by the modern command dispatcher with the sender's own source, so permissions and output are the host's. Mods subclass the original
 * command classes to hook {@code execute} or only name them (a server command list, a wrapper that delegates to {@code super}).
 */
public abstract class ServerCommandDelegate extends CommandBase {
    private final String name;
    private final int level;

    protected ServerCommandDelegate(String name, int level) {
        this.name = name;
        this.level = level;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return level;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands." + name + ".usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        var m = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (m == null) return;
        String line = name + (args.length == 0 ? "" : " " + String.join(" ", args));
        var entity = sender == null ? null : sender.getCommandSenderEntity();
        var source = entity != null && entity.mcc$entity != null ? entity.mcc$entity.createCommandSourceStack() : m.createCommandSourceStack();
        m.getCommands().performPrefixedCommand(source, line);
    }
}
