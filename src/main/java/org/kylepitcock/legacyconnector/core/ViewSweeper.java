package org.kylepitcock.legacyconnector.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy views of modern objects (an entity's legacy Entity, a block entity's TileEntity) are cached per object so legacy code
 * sees a stable identity. The caches have weak keys, but every view holds its modern object, so an entry is never released on
 * its own: every entity and block entity a legacy mod ever looked at stayed in memory. This sweeper drops the entries whose
 * modern object the game has removed (killed, discarded, broken, or unloaded with its chunk; it is not used again after that).
 */
public final class ViewSweeper {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Views");
    private static final List<Entry<?>> CACHES = new CopyOnWriteArrayList<>();
    private static Thread thread;

    private record Entry<K>(String name, Map<K, ?> map, Predicate<K> removed) {
        int sweep() {
            int before = map.size();
            // the maps are concurrent or synchronized; removeIf on the key set holds the map's lock for synchronized ones
            synchronized (map) {
                map.keySet().removeIf(k -> {
                    try {
                        return removed.test(k);
                    } catch (RuntimeException e) {
                        return false;
                    }
                });
            }
            return before - map.size();
        }
    }

    private ViewSweeper() {}

    public static synchronized <K> void register(String name, Map<K, ?> map, Predicate<K> removed) {
        CACHES.add(new Entry<>(name, map, removed));
        if (thread == null) {
            thread = new Thread(ViewSweeper::run, "Legacy Connector view sweeper");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }

    private static void run() {
        int reports = 0;
        while (true) {
            try {
                Thread.sleep(20_000);
                for (Entry<?> e : CACHES) {
                    int dropped = e.sweep();
                    if (dropped > 1000 && reports++ < 10) LOG.debug("Dropped {} stale {} views", dropped, e.name());
                }
            } catch (InterruptedException e) {
                return;
            } catch (RuntimeException e) {
                LOG.debug("View sweep failed", e);
            }
        }
    }
}
