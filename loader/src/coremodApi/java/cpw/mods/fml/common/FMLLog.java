package cpw.mods.fml.common;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FMLLog {
    public static void log(String targetLog, Level level, String format, Object... data) {
        FMLRelaunchLog.log(targetLog, level, format, data);
    }

    public static void log(Level level, String format, Object... data) {
        FMLRelaunchLog.log(level, format, data);
    }

    public static void log(String targetLog, Level level, Throwable ex, String format, Object... data) {
        FMLRelaunchLog.log(targetLog, level, ex, format, data);
    }

    public static void log(Level level, Throwable ex, String format, Object... data) {
        FMLRelaunchLog.log(level, ex, format, data);
    }

    public static void severe(String format, Object... data) {
        log(Level.ERROR, format, data);
    }

    public static void bigWarning(String format, Object... data) {
        log(Level.WARN, format, data);
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
        return LogManager.getLogger("FML");
    }

    public static void makeLog(String logChannel) {
    }
}
