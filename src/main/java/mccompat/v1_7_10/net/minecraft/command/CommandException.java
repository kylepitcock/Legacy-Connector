package mccompat.v1_7_10.net.minecraft.command;

public class CommandException extends RuntimeException {
    private final Object[] errorObjects;

    public CommandException(String message, Object... args) {
        super(message);
        this.errorObjects = args;
    }

    public Object[] getErrorOjbects() {
        return errorObjects;
    }
}
