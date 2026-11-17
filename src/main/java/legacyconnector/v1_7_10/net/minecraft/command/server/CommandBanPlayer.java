package legacyconnector.v1_7_10.net.minecraft.command.server;

/** {@code /ban <player> [reason]}: run by the modern command of the same name. */
public class CommandBanPlayer extends ServerCommandDelegate {
    public CommandBanPlayer() {
        super("ban", 3);
    }
}
