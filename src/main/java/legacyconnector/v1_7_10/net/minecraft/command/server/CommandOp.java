package legacyconnector.v1_7_10.net.minecraft.command.server;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.command.ICommandSender;

/** {@code /op <player>} (1.7.10 {@code CommandOp}): run by the modern command of the same name; tab completion offers the players who are not operators yet. */
public class CommandOp extends ServerCommandDelegate {
    public CommandOp() {
        super("op", 3);
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        var server = legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer.getServer();
        if (args.length != 1 || server == null) return null;
        java.util.List<String> names = new java.util.ArrayList<>();
        for (String name : server.getAllUsernames()) {
            if (!mcc$isOperator(name) && doesStringStartWith(args[0], name)) names.add(name);
        }
        return names;
    }
}
