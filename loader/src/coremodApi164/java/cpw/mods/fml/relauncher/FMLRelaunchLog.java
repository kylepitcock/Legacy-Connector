package cpw.mods.fml.relauncher;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Forge 1.6.4's FMLRelaunchLog: java.util.logging. */
public class FMLRelaunchLog {
    public static FMLRelaunchLog log = new FMLRelaunchLog();
    public static Side side = FMLLaunchHandler.side();
    private static final Logger LOG = Logger.getLogger("ForgeModLoader");

    public static void log(String targetLog, Level level, String format, Object... data) {
        Logger.getLogger(targetLog).log(level, String.format(format, data));
    }

    public static void log(Level level, String format, Object... data) {
        LOG.log(level, String.format(format, data));
    }

    public static void log(String targetLog, Level level, Throwable ex, String format, Object... data) {
        Logger.getLogger(targetLog).log(level, String.format(format, data), ex);
    }

    public static void log(Level level, Throwable ex, String format, Object... data) {
        LOG.log(level, String.format(format, data), ex);
    }

    public static void severe(String format, Object... data) {
        log(Level.SEVERE, format, data);
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

    public Logger getLogger() {
        return LOG;
    }

    public static void makeLog(String logChannel) {
    }
}
