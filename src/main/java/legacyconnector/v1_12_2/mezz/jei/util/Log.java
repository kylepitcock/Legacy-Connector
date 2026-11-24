package legacyconnector.v1_12_2.mezz.jei.util;

/** JEI 4 (Minecraft 1.12.2) internals some plugins reach into; here a small working copy. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public final class Log {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("jei");

    private Log() {}

    public static org.apache.logging.log4j.Logger get() {
        return LOGGER;
    }
}
