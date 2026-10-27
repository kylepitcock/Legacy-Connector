package legacyconnector.v1_12_2.net.minecraft.command;

public class EntityNotFoundException extends CommandException {
    public EntityNotFoundException(String entity) {
        this("commands.generic.entity.notFound", entity);
    }

    public EntityNotFoundException(String message, Object... args) {
        super(message, args);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
