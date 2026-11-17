package legacyconnector.v1_7_10.net.minecraft.command.server;

/** {@code /whitelist add|remove|list|on|off|reload}: run by the modern command of the same name. */
public class CommandWhitelist extends ServerCommandDelegate {
    public CommandWhitelist() {
        super("whitelist", 3);
    }
}
