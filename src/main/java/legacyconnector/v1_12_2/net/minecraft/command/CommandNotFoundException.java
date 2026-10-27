package legacyconnector.v1_12_2.net.minecraft.command;

public class CommandNotFoundException extends CommandException {
    public CommandNotFoundException() {
        this("commands.generic.notFound");
    }

    public CommandNotFoundException(String message, Object... args) {
        super(message, args);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
