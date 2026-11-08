package legacyconnector.v1_7_10.cpw.mods.fml.relauncher;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** FML's early log (the "FML" logger). */
public class FMLRelaunchLog {
    public static final FMLRelaunchLog log = new FMLRelaunchLog();
    public static Side side = FMLLaunchHandler.side();
    private final Logger myLog = LogManager.getLogger("FML");

    /** Makes sure the named log channel exists (1.6.4 FML created its file handler here). */
    public static void makeLog(String logChannel) {
        LogManager.getLogger(logChannel);
    }

    public static void configureLogging() {
    }

    public Logger getLogger() {
        return myLog;
    }

    public static void log(String targetLog, Level level, String format, Object... data) {
        LogManager.getLogger(targetLog).log(level, String.format(format, data));
    }

    public static void log(Level level, String format, Object... data) {
        log.myLog.log(level, String.format(format, data));
    }

    public static void log(String targetLog, Level level, Throwable ex, String format, Object... data) {
        LogManager.getLogger(targetLog).log(level, String.format(format, data), ex);
    }

    public static void log(Level level, Throwable ex, String format, Object... data) {
        log.myLog.log(level, String.format(format, data), ex);
    }


    /** 1.4.7-1.6.4 logged with java.util.logging levels. */
    public static org.apache.logging.log4j.Level mcc$level(java.util.logging.Level level) {
        int v = level == null ? 800 : level.intValue();
        if (v >= 1000) return Level.ERROR;
        if (v >= 900) return Level.WARN;
        if (v >= 800) return Level.INFO;
        if (v >= 500) return Level.DEBUG;
        return Level.TRACE;
    }

    public static void log(String targetLog, java.util.logging.Level level, String format, Object... data) {
        log(targetLog, mcc$level(level), format, data);
    }

    public static void log(java.util.logging.Level level, String format, Object... data) {
        log(mcc$level(level), format, data);
    }

    public static void log(String targetLog, java.util.logging.Level level, Throwable ex, String format, Object... data) {
        log(targetLog, mcc$level(level), ex, format, data);
    }

    public static void log(java.util.logging.Level level, Throwable ex, String format, Object... data) {
        log(mcc$level(level), ex, format, data);
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
}
