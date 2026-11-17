package legacyconnector.v1_12_2.net.minecraft.command.server;

/** {@code /banlist [ips|players]}: run by the modern command of the same name. */
public class CommandListBans extends ServerCommandDelegate {
    public CommandListBans() {
        super("banlist", 3);
    }
}
