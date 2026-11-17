package legacyconnector.v1_12_2.net.minecraft.command.server;

/** {@code /me <action>}: run by the modern command of the same name. */
public class CommandEmote extends ServerCommandDelegate {
    public CommandEmote() {
        super("me", 0);
    }
}
