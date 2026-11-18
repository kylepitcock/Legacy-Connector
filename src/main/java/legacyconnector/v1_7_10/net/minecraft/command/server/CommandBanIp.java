package legacyconnector.v1_7_10.net.minecraft.command.server;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;

/** {@code /ban-ip <address|player> [reason]} (1.7.10 {@code CommandBanIp}): run by the modern command of the same name. */
public class CommandBanIp extends ServerCommandDelegate {
    public CommandBanIp() {
        super("ban-ip", 3);
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        var server = legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer.getServer();
        return args.length == 1 && server != null ? getListOfStringsMatchingLastWord(args, server.getAllUsernames()) : null;
    }
}
