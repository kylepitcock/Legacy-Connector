package legacyconnector.v1_12_2.net.minecraft.command.server;

/** {@code /op <player>}: run by the modern command of the same name. */
public class CommandOp extends ServerCommandDelegate {
    public CommandOp() {
        super("op", 3);
    }
}
