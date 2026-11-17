package legacyconnector.v1_7_10.net.minecraft.command.server;

/** {@code /pardon <player>}: run by the modern command of the same name. */
public class CommandPardonPlayer extends ServerCommandDelegate {
    public CommandPardonPlayer() {
        super("pardon", 3);
    }
}
