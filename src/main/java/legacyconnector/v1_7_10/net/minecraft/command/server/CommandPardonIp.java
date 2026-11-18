package legacyconnector.v1_7_10.net.minecraft.command.server;

/** {@code /pardon-ip <address>} (1.7.10 {@code CommandPardonIp}): run by the modern command of the same name. */
public class CommandPardonIp extends ServerCommandDelegate {
    public CommandPardonIp() {
        super("pardon-ip", 3);
    }
}
