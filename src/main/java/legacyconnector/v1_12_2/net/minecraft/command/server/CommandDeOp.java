package legacyconnector.v1_12_2.net.minecraft.command.server;

/** {@code /deop <player>}: run by the modern command of the same name. */
public class CommandDeOp extends ServerCommandDelegate {
    public CommandDeOp() {
        super("deop", 3);
    }
}
