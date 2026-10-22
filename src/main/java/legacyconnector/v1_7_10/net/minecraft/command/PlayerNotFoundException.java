package legacyconnector.v1_7_10.net.minecraft.command;

public class PlayerNotFoundException extends CommandException {
    public PlayerNotFoundException() {
        this("commands.generic.player.notFound");
    }

    public PlayerNotFoundException(String message, Object... args) {
        super(message, args);
    }
}
