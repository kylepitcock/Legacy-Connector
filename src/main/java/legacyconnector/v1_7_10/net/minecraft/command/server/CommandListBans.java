package legacyconnector.v1_7_10.net.minecraft.command.server;

/** {@code /banlist [ips]}: run by the modern command of the same name. */
public class CommandListBans extends ServerCommandDelegate {
    public CommandListBans() {
        super("banlist", 3);
    }
}
