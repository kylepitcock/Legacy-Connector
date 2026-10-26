package legacyconnector.v1_7_10.net.minecraft.command;

/** No command has the name that was typed ("Unknown command. Try /help for a list of commands"). */
public class CommandNotFoundException extends CommandException {
    public CommandNotFoundException() {
        this("commands.generic.notFound");
    }

    public CommandNotFoundException(String message, Object... args) {
        super(message, args);
    }
}
