package legacyconnector.v1_7_10.net.minecraft.command;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.command.server.ServerCommandDelegate;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;

/**
 * {@code /gamemode <mode> [player]} (1.7.10 {@code CommandGameMode}): the mode is a number (0 survival, 1 creative, 2 adventure) or a name or its
 * first letter; the modern command of the same name runs it with the sender's permissions.
 */
public class CommandGameMode extends ServerCommandDelegate {
    public CommandGameMode() {
        super("gamemode", 2);
    }

    @Override
    protected String[] mcc$translateArguments(String[] args) {
        if (args.length == 0) return args;
        String[] out = args.clone();
        out[0] = getGameModeFromCommand(out[0]);
        return out;
    }

    /** The mode name a 1.7.10 argument means: {@code 0}/{@code s}, {@code 1}/{@code c}, {@code 2}/{@code a} or the full name; anything else is passed on. */
    protected String getGameModeFromCommand(String text) {
        String t = text.toLowerCase(java.util.Locale.ROOT);
        return switch (t) {
            case "0", "s", "survival" -> "survival";
            case "1", "c", "creative" -> "creative";
            case "2", "a", "adventure" -> "adventure";
            case "3", "sp", "spectator" -> "spectator";
            default -> text;
        };
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "survival", "creative", "adventure");
        return args.length == 2 ? getListOfStringsMatchingLastWord(args, getListOfPlayerUsernames()) : null;
    }

    protected String[] getListOfPlayerUsernames() {
        MinecraftServer server = MinecraftServer.getServer();
        return server == null ? new String[0] : server.getAllUsernames();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 1;
    }
}
