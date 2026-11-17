package legacyconnector.v1_7_10.net.minecraft.command.server;

/** {@code /tp [player] <target | x y z>}: run by the modern command of the same name. */
public class CommandTeleport extends ServerCommandDelegate {
    public CommandTeleport() {
        super("tp", 2);
    }
}
