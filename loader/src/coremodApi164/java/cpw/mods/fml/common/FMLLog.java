package cpw.mods.fml.common;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Forge 1.6.4's FMLLog: java.util.logging (1.7.x moved to log4j). */
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
        log(Level.SEVERE, format, data);
    }

    public static void bigWarning(String format, Object... data) {
        log(Level.WARNING, format, data);
    }

    public static void warning(String format, Object... data) {
        log(Level.WARNING, format, data);
    }

    public static void info(String format, Object... data) {
        log(Level.INFO, format, data);
    }

    public static void fine(String format, Object... data) {
        log(Level.FINE, format, data);
    }

    public static void finer(String format, Object... data) {
        log(Level.FINER, format, data);
    }

    public static void finest(String format, Object... data) {
        log(Level.FINEST, format, data);
    }

    public static Logger getLogger() {
        return Logger.getLogger("ForgeModLoader");
    }

    public static void makeLog(String logChannel) {
    }
}
