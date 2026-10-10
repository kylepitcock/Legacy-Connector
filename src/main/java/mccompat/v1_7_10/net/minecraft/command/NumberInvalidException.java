package mccompat.v1_7_10.net.minecraft.command;

public class NumberInvalidException extends CommandException {
    public NumberInvalidException() {
        this("commands.generic.num.invalid");
    }

    public NumberInvalidException(String message, Object... args) {
        super(message, args);
    }
}
