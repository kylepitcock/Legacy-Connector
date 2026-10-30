package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Questions FML asked the player while a world loaded (a mod's data conversion, missing blocks). The modern game has no such dialog:
 * questions are logged and answered yes, notices logged; {@link #abort} stops loading the world.
 */
public class StartupQuery {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/StartupQuery");

    private StartupQuery() {
    }

    public static boolean confirm(String text) {
        LOG.warn("A mod asked to confirm (answered yes): {}", text);
        return true;
    }

    public static void notify(String text) {
        LOG.warn("A mod's startup notice: {}", text);
    }

    public static void abort() {
        throw new AbortedException();
    }

    public static boolean check() {
        return true;
    }

    public static class AbortedException extends RuntimeException {
        private static final long serialVersionUID = -1L;

        private AbortedException() {
            super("A mod aborted loading the world");
        }
    }
}
