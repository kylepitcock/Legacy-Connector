package legacyconnector.v1_6_4.facade.cpw.mods.fml.common;

/** Facade of the 1.7.10 shim's {@code FMLLog} for 1.6.4 mods: 1.6.4's {@code getLogger()} was a {@code java.util.logging.Logger}. */
public final class FMLLog {
    private FMLLog() {}

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("ForgeModLoader");

    public static java.util.logging.Logger getLogger() {
        return LOGGER;
    }
    /** 1.6.4 {@code log(channel, level, format, data...)}: one logger, the channel is a prefix. */
    public static void log(String channel, java.util.logging.Level level, String format, Object... data) {
        LOGGER.log(level, "[" + channel + "] " + String.format(format, data));
    }

    /** 1.6.4 {@code log(level, throwable, format, data...)}. */
    public static void log(java.util.logging.Level level, Throwable ex, String format, Object... data) {
        LOGGER.log(level, String.format(format, data), ex);
    }
}
