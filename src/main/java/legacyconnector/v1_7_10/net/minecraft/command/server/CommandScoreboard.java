package legacyconnector.v1_7_10.net.minecraft.command.server;

/**
 * {@code /scoreboard objectives|players|teams ...} (1.7.10 {@code CommandScoreboard}): run by the modern command of the same name. The
 * sub-commands keep their 1.7.10 spelling ({@code objectives add}, {@code players set}, {@code teams join}); the criteria and options only the
 * old game had are rejected by the modern parser with its own message.
 */
public class CommandScoreboard extends ServerCommandDelegate {
    public CommandScoreboard() {
        super("scoreboard", 2);
    }
}
