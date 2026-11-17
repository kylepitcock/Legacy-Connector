package legacyconnector.v1_7_10.net.minecraft.command.server;

/** {@code /summon <entity> [x y z]}: run by the modern command of the same name. */
public class CommandSummon extends ServerCommandDelegate {
    public CommandSummon() {
        super("summon", 2);
    }
}
