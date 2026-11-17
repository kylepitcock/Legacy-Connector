package legacyconnector.v1_12_2.net.minecraft.command.server;

/** {@code /msg <player> <message>}: run by the modern command of the same name. */
public class CommandMessage extends ServerCommandDelegate {
    public CommandMessage() {
        super("msg", 0);
    }
}
