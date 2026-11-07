package legacyconnector.v1_6_4.net.minecraft.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/** 1.6.4's {@code LogAgent}: an {@link ILogAgent} writing to a {@code java.util.logging} logger (the one FML's log wrote to). */
public class LogAgent implements ILogAgent {
    private static final LogAgent WORLD = new LogAgent(Logger.getLogger("Minecraft"));

    private final Logger logger;

    public LogAgent(Logger logger) {
        this.logger = logger;
    }

    /** The agent every world returns from {@code getWorldLogAgent()}. */
    public static LogAgent world() {
        return WORLD;
    }

    @Override
    public Logger func_120013_a() {
        return logger;
    }

    @Override
    public void logInfo(String message) {
        logger.log(Level.INFO, message);
    }

    @Override
    public void logWarning(String message) {
        logger.log(Level.WARNING, message);
    }

    @Override
    public void logWarningFormatted(String message, Object... args) {
        logger.log(Level.WARNING, message, args);
    }

    @Override
    public void logWarningException(String message, Throwable t) {
        logger.log(Level.WARNING, message, t);
    }

    @Override
    public void logSevere(String message) {
        logger.log(Level.SEVERE, message);
    }

    @Override
    public void logSevereException(String message, Throwable t) {
        logger.log(Level.SEVERE, message, t);
    }

    @Override
    public void logFine(String message) {
        logger.log(Level.FINE, message);
    }
}
