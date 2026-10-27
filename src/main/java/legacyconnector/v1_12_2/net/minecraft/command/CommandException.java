package legacyconnector.v1_12_2.net.minecraft.command;

/** A command's failure: a translation key and its arguments, shown to the sender in red. */
public class CommandException extends Exception {
    private final Object[] errorObjects;

    public CommandException(String message, Object... objects) {
        super(message);
        this.errorObjects = objects == null ? new Object[0] : objects;
    }

    public Object[] getErrorObjects() {
        return errorObjects;
    }

    /** Command errors carry no stack trace (vanilla skipped filling it in). */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
