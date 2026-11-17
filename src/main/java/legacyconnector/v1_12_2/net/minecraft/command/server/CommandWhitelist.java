package legacyconnector.v1_12_2.net.minecraft.command.server;

/** {@code /whitelist}: run by the modern command of the same name. */
public class CommandWhitelist extends ServerCommandDelegate {
    public CommandWhitelist() {
        super("whitelist", 3);
    }
}
