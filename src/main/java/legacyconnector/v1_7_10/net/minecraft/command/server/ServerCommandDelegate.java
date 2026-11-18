package legacyconnector.v1_7_10.net.minecraft.command.server;

import legacyconnector.v1_7_10.net.minecraft.command.CommandBase;
import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;

/**
 * Base of the 1.7.10 server commands the modern game already implements ({@code ban}, {@code tp}, {@code whitelist}...): the line is
 * run by the modern command dispatcher with the sender's own source, so permissions and output are the host's. Mods subclass the
 * original command classes to hook {@code processCommand} or only name them.
 */
public abstract class ServerCommandDelegate extends CommandBase {
    private final String name;
    private final int level;
    private final String usage;

    protected ServerCommandDelegate(String name, int level) {
        this.name = name;
        this.level = level;
        this.usage = "commands." + name + ".usage";
    }

    /** Whether the online player of that name is an operator. */
    protected static boolean mcc$isOperator(String name) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        var player = server.getPlayerList().getPlayerByName(name);
        return player != null && server.getPlayerList().isOp(player.getGameProfile());
    }

    /** Rewrites the 1.7.10 arguments into what the modern command takes (identity by default). */
    protected String[] mcc$translateArguments(String[] args) {
        return args;
    }

    @Override
    public String getCommandName() {
        return name;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return level;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return usage;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        args = mcc$translateArguments(args);
        String line = name + (args.length == 0 ? "" : " " + String.join(" ", args));
        var source = sender instanceof legacyconnector.v1_7_10.net.minecraft.entity.Entity e && e.mcc$entity() != null
                ? e.mcc$entity().createCommandSourceStack() : server.createCommandSourceStack();
        server.getCommands().performPrefixedCommand(source, line);
    }
}
