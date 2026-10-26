package legacyconnector.v1_7_10.net.minecraft.launchwrapper;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** LaunchWrapper's logger: {@code String.format} messages on the "LaunchWrapper" log4j logger or a named channel. */
public class LogWrapper {
    public static LogWrapper log = new LogWrapper();
    private Logger myLog = LogManager.getLogger("LaunchWrapper");

    public static void retarget(Logger to) {
        log.myLog = to;
    }

    public static void log(String logChannel, Level level, String format, Object... data) {
        makeLog(logChannel);
        LogManager.getLogger(logChannel).log(level, format(format, data));
    }

    public static void log(Level level, String format, Object... data) {
        log.myLog.log(level, format(format, data));
    }

    public static void log(String logChannel, Level level, Throwable ex, String format, Object... data) {
        makeLog(logChannel);
        LogManager.getLogger(logChannel).log(level, format(format, data), ex);
    }

    public static void log(Level level, Throwable ex, String format, Object... data) {
        log.myLog.log(level, format(format, data), ex);
    }

    public static void severe(String format, Object... data) {
        log(Level.ERROR, format, data);
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

    public static void finest(String format, Object... data) {
        log(Level.TRACE, format, data);
    }

    public static void makeLog(String logChannel) {
        LogManager.getLogger(logChannel);
    }

    private static String format(String format, Object... data) {
        if (data == null || data.length == 0) return format;
        try {
            return String.format(format, data);
        } catch (java.util.IllegalFormatException e) {
            return format;
        }
    }
}
