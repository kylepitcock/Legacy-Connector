package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Throttled logging for failures inside legacy 1.12.2 rendering code (it runs every frame: one line per cause, then silence). */
public final class RenderErrors1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Render1122");
    private static final Map<String, Integer> SEEN = new ConcurrentHashMap<>();

    private RenderErrors1122() {}

    public static void report(String what, Throwable t) {
        String key = what + "|" + t.getClass().getName() + "|" + (t.getStackTrace().length > 0 ? t.getStackTrace()[0] : "");
        int n = SEEN.merge(key, 1, Integer::sum);
        if (n == 1) LOG.warn("Legacy rendering failed while {} (further failures of this kind are not logged)", what, t);
    }
}
