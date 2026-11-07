package legacyconnector.v1_6_4.facade.cpw.mods.fml.common;

/** Facade of the 1.7.10 shim's {@code FMLLog} for 1.6.4 mods: 1.6.4's {@code getLogger()} was a {@code java.util.logging.Logger}. */
public final class FMLLog {
    private FMLLog() {}

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("ForgeModLoader");

    public static java.util.logging.Logger getLogger() {
        return LOGGER;
    }
}
