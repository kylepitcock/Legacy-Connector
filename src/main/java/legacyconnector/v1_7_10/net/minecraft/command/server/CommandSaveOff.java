package legacyconnector.v1_7_10.net.minecraft.command.server;

import legacyconnector.v1_7_10.net.minecraft.command.CommandBase;
import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;

/** {@code /save-off}: stops world saving. */
public class CommandSaveOff extends CommandBase {
    public CommandSaveOff() {
    }

    @Override
    public String getCommandName() {
        return "save-off";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.save-off.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        MinecraftServer server = MinecraftServer.getServer();
        for (net.minecraft.server.level.ServerLevel level : server.mcc$modern().getAllLevels()) level.noSave = true;
        notifyOperators(sender, this, "commands.save.disabled");
    }
}
