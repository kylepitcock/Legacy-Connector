package org.kylepitcock.legacyconnector.core;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy mods allocate direct buffers freely (LWJGL 2's BufferUtils in render code, every frame). Direct memory is only
 * returned when the heap is collected, and with a large heap that can take long enough for native memory to reach many
 * gigabytes and the machine to swap (frames below one per second, saves taking seconds per dimension). This watchdog asks
 * for a collection when the direct pool passes a threshold, at most every 15 seconds.
 */
public final class DirectMemoryGuard {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Memory");
    private static final long THRESHOLD = Long.getLong("legacyconnector.directMemoryGcMb", 1024) * 1024 * 1024;
    private static boolean started;

    private DirectMemoryGuard() {}

    public static synchronized void start() {
        if (started || THRESHOLD <= 0) return;
        started = true;
        BufferPoolMXBean direct = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class).stream()
                .filter(b -> b.getName().equals("direct")).findFirst().orElse(null);
        if (direct == null) return;
        Thread t = new Thread(() -> watch(direct), "Legacy Connector direct memory guard");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /** Every 10 s, heap and direct memory in the log (-Dlegacyconnector.memoryLog=true, and during the screenshot test). */
    private static final boolean MEMORY_LOG = Boolean.getBoolean("legacyconnector.memoryLog") || System.getProperty("legacyconnector.shotScenario") != null;

    private static void watch(BufferPoolMXBean direct) {
        int reported = 0;
        int ticks = 0;
        while (true) {
            try {
                Thread.sleep(5000);
                long used = direct.getMemoryUsed();
                if (MEMORY_LOG && ++ticks % 2 == 0) {
                    Runtime rt = Runtime.getRuntime();
                    LOG.info("Memory: heap {} / {} MB, direct buffers {} MB ({} buffers)", (rt.totalMemory() - rt.freeMemory()) >> 20,
                            rt.maxMemory() >> 20, used >> 20, direct.getCount());
                }
                if (used < THRESHOLD) continue;
                System.gc();
                Thread.sleep(10000);
                if (reported++ < 5) {
                    LOG.info("Direct buffer memory reached {} MB; collected garbage, now {} MB", used >> 20, direct.getMemoryUsed() >> 20);
                }
            } catch (InterruptedException e) {
                return;
            } catch (RuntimeException e) {
                LOG.debug("Direct memory check failed", e);
            }
        }
    }
}
