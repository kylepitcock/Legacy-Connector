package legacyconnector.v1_12_2.net.minecraft.command.server;

/** {@code /say <message>}: run by the modern command of the same name. */
public class CommandBroadcast extends ServerCommandDelegate {
    public CommandBroadcast() {
        super("say", 1);
    }
}
