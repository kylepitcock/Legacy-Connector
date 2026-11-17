package legacyconnector.v1_12_2.net.minecraft.command.server;

/** {@code /pardon-ip <address>}: run by the modern command of the same name. */
public class CommandPardonIp extends ServerCommandDelegate {
    public CommandPardonIp() {
        super("pardon-ip", 3);
    }
}
