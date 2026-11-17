package legacyconnector.v1_12_2.net.minecraft.command.server;

/** {@code /kick <player> [reason]}: run by the modern command of the same name. */
public class CommandServerKick extends ServerCommandDelegate {
    public CommandServerKick() {
        super("kick", 3);
    }
}
