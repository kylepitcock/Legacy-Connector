package legacyconnector.v1_12_2.net.minecraft.command.server;

/** {@code /ban-ip <address|player> [reason]}: run by the modern command of the same name. */
public class CommandBanIp extends ServerCommandDelegate {
    public CommandBanIp() {
        super("ban-ip", 3);
    }
}
