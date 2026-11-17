package org.kylepitcock.legacyconnector.core.worldgen;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Dev/test guard: legacy code that runs during chunk generation must reach blocks through the generation region, never the real
 * level (a read or write on the real level waits for the chunk being generated and deadlocks the server). The shims call
 * {@link #reached} where they hand out the real level while a thread routes its world to a region; with the guard on (server self
 * test, or {@code -Dlegacyconnector.realLevelGuard=true}) each distinct call site is logged once with a short stack.
 * Not every report is a defect (creating an entity needs the level object); the log line says where to look.
 */
public final class RealLevelGuard {
    private static final Logger LOG = LogManager.getLogger("LegacyConnector/RealLevelGuard");
    private static final boolean ON = Boolean.getBoolean("legacyconnector.realLevelGuard") || Boolean.getBoolean("legacytest.autostop");
    private static final ConcurrentHashMap<String, Boolean> SEEN = new ConcurrentHashMap<>();
    private static final AtomicInteger SITES = new AtomicInteger();

    private RealLevelGuard() {}

    public static boolean enabled() {
        return ON;
    }

    /** Called while the thread's world is a generation region and the caller asked for the real level. */
    public static void reached(String accessor) {
        if (!ON) return;
        StackTraceElement[] st = new Throwable().getStackTrace();
        StackTraceElement site = null;
        int from = 0;
        for (int i = 1; i < st.length; i++) {
            String c = st[i].getClassName();
            if (c.startsWith("legacyconnector.") && c.contains(".net.minecraft.world.World")) continue;
            if (c.equals(RealLevelGuard.class.getName())) continue;
            site = st[i];
            from = i;
            break;
        }
        if (site == null) return;
        String key = accessor + "@" + site.getClassName() + "." + site.getMethodName() + ":" + site.getLineNumber();
        if (SEEN.putIfAbsent(key, Boolean.TRUE) != null) return;
        int n = SITES.incrementAndGet();
        if (n > 200) return;
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < Math.min(st.length, from + 6); i++) sb.append("\n    at ").append(st[i]);
        LOG.warn("REAL LEVEL GUARD: {} reached the real level during chunk generation (site #{}): {}{}", accessor, n, key, sb);
    }
}
