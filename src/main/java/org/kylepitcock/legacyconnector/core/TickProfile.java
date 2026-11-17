package org.kylepitcock.legacyconnector.core;

import java.util.Arrays;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Measurement aid (off unless {@code -Dlegacyconnector.msptLog=<ticks per line>}, e.g. through {@code -PmsptLog=600}): logs the mean, 50th, 99th
 * percentile and maximum server tick time every N ticks, plus the heap in use, so a run can be compared before and after a change.
 */
public final class TickProfile {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Perf");

    private TickProfile() {}

    public static void register(IEventBus game) {
        int window = Integer.getInteger("legacyconnector.msptLog", 0);
        if (window <= 0) return;
        long[] times = new long[window];
        int[] n = {0};
        long[] start = {0};
        game.addListener((ServerTickEvent.Pre e) -> start[0] = System.nanoTime());
        game.addListener((ServerTickEvent.Post e) -> {
            times[n[0]++] = System.nanoTime() - start[0];
            if (n[0] < window) return;
            n[0] = 0;
            long[] s = times.clone();
            Arrays.sort(s);
            long sum = 0;
            for (long t : s) sum += t;
            Runtime rt = Runtime.getRuntime();
            LOG.info("MSPT over {} ticks: mean {} p50 {} p99 {} max {} ms, heap {} MB", window, fmt(sum / window), fmt(s[window / 2]),
                    fmt(s[Math.min(window - 1, (int) (window * 0.99))]), fmt(s[window - 1]), (rt.totalMemory() - rt.freeMemory()) >> 20);
        });
    }

    private static String fmt(long nanos) {
        return String.format(java.util.Locale.ROOT, "%.2f", nanos / 1_000_000.0);
    }
}
