package mccompat.v1_7_10.net.minecraft.command;

public class SyntaxErrorException extends CommandException {
    public SyntaxErrorException() {
        this("commands.generic.snytax");
    }

    public SyntaxErrorException(String message, Object... args) {
        super(message, args);
    }
}
