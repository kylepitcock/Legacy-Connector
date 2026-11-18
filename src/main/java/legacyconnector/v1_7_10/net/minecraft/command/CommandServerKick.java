package legacyconnector.v1_7_10.net.minecraft.command;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.command.server.ServerCommandDelegate;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;

/** {@code /kick <player> [reason]} (1.7.10 {@code CommandServerKick}): run by the modern command of the same name. */
public class CommandServerKick extends ServerCommandDelegate {
    public CommandServerKick() {
        super("kick", 3);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return MinecraftServer.getServer() != null && super.canCommandSenderUseCommand(sender);
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        MinecraftServer server = MinecraftServer.getServer();
        return args.length >= 1 && server != null ? getListOfStringsMatchingLastWord(args, server.getAllUsernames()) : null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }
}
