package legacyconnector.v1_7_10.net.minecraft.command.server;

import legacyconnector.v1_7_10.net.minecraft.command.CommandBase;
import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;

/** {@code /save-all [flush]}: saves the players and worlds. */
public class CommandSaveAll extends CommandBase {
    public CommandSaveAll() {
    }

    @Override
    public String getCommandName() {
        return "save-all";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.save.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        MinecraftServer server = MinecraftServer.getServer();
        notifyOperators(sender, this, "commands.save.start");
        server.mcc$modern().saveEverything(true, args.length > 0 && "flush".equals(args[0]), true);
        notifyOperators(sender, this, "commands.save.success");
    }
}
