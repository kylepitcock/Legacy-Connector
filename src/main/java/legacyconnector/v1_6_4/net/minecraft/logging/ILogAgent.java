package legacyconnector.v1_6_4.net.minecraft.logging;

import java.util.logging.Logger;

/** 1.6.4's {@code ILogAgent}: the logger a world, the server and packets write to (removed in 1.7 for log4j). */
public interface ILogAgent {
    Logger func_120013_a();

    void logInfo(String message);

    void logWarning(String message);

    void logWarningFormatted(String message, Object... args);

    void logWarningException(String message, Throwable t);

    void logSevere(String message);

    void logSevereException(String message, Throwable t);

    void logFine(String message);
}
