package legacyconnector.v1_12_2.net.minecraft.command.server;

/** {@code /scoreboard}: run by the modern command of the same name. */
public class CommandScoreboard extends ServerCommandDelegate {
    public CommandScoreboard() {
        super("scoreboard", 2);
    }
}
