package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class FMLLog {
    private static final Logger LOG = LogManager.getLogger("FML");

    private FMLLog() {}

    public static void log(String targetLog, Level level, String format, Object... data) {
        LogManager.getLogger(targetLog).log(level, safeFormat(format, data));
    }

    public static void log(Level level, String format, Object... data) {
        LOG.log(level, safeFormat(format, data));
    }

    public static void log(String targetLog, Level level, Throwable ex, String format, Object... data) {
        LogManager.getLogger(targetLog).log(level, safeFormat(format, data), ex);
    }

    public static void log(Level level, Throwable ex, String format, Object... data) {
        LOG.log(level, safeFormat(format, data), ex);
    }

    public static void severe(String format, Object... data) {
        log(Level.ERROR, format, data);
    }

    public static void bigWarning(String format, Object... data) {
        log(Level.WARN, "****************************************");
        log(Level.WARN, "* " + format, data);
        log(Level.WARN, "****************************************");
    }

    public static void warning(String format, Object... data) {
        log(Level.WARN, format, data);
    }

    public static void info(String format, Object... data) {
        log(Level.INFO, format, data);
    }

    public static void fine(String format, Object... data) {
        log(Level.DEBUG, format, data);
    }

    public static void finer(String format, Object... data) {
        log(Level.TRACE, format, data);
    }

    public static Logger getLogger() {
        return LOG;
    }

    /** Legacy FMLLog used String.format semantics; malformed patterns must not crash the caller. */
    private static String safeFormat(String format, Object... data) {
        if (data == null || data.length == 0) return format;
        try {
            return String.format(format, data);
        } catch (RuntimeException e) {
            return format + " " + java.util.Arrays.toString(data);
        }
    }
}
