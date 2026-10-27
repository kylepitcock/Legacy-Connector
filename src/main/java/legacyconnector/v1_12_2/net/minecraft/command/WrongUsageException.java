package legacyconnector.v1_12_2.net.minecraft.command;

/** Shown as "Usage: " followed by the message. */
public class WrongUsageException extends SyntaxErrorException {
    public WrongUsageException(String message, Object... replacements) {
        super(message, replacements);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
