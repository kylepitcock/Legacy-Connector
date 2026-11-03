package legacyconnector.v1_7_10.net.minecraft.command.server;

import legacyconnector.v1_7_10.net.minecraft.command.CommandBase;
import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;

/** {@code /save-on}: resumes world saving. */
public class CommandSaveOn extends CommandBase {
    public CommandSaveOn() {
    }

    @Override
    public String getCommandName() {
        return "save-on";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.save-on.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        MinecraftServer server = MinecraftServer.getServer();
        for (net.minecraft.server.level.ServerLevel level : server.mcc$modern().getAllLevels()) level.noSave = false;
        notifyOperators(sender, this, "commands.save.enabled");
    }
}
