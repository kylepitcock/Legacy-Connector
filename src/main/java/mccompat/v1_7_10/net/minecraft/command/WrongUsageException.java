package mccompat.v1_7_10.net.minecraft.command;

public class WrongUsageException extends SyntaxErrorException {
    public WrongUsageException(String message, Object... args) {
        super(message, args);
    }
}
