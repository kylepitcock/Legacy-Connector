package legacyconnector.v1_12_2.net.minecraftforge.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Background work spread over server ticks (mods' world pregenerators and converters): after each server tick the workers run
 * for up to the rest of the tick's 50 ms, one {@code doWork} at a time, until they report they are done.
 */
public class WorldWorkerManager {
    private static final List<IWorker> workers = new ArrayList<>();
    private static long startTime = -1;
    private static int index = 0;
    private static boolean listening;

    public static synchronized void addWorker(IWorker worker) {
        workers.add(worker);
        if (!listening) {
            listening = true;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Pre e) -> {
                startTime = System.currentTimeMillis();
            });
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
                tick(true);
            });
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> clear());
        }
    }

    public static synchronized void tick(boolean start) {
        if (start) {
            if (startTime < 0) startTime = System.currentTimeMillis();
            long time = 50 - (System.currentTimeMillis() - startTime);
            if (time < 10) time = 10;
            time += System.currentTimeMillis();
            while (System.currentTimeMillis() < time && !workers.isEmpty()) {
                IWorker task = getNext();
                if (task == null) break;
                boolean again;
                try {
                    again = task.doWork();
                } catch (RuntimeException | LinkageError e) {
                    org.slf4j.LoggerFactory.getLogger("LegacyConnector/1.12.2").warn("A world worker failed and was removed", e);
                    again = false;
                }
                if (!task.hasWork()) {
                    remove(task);
                } else if (!again) {
                    break;
                }
            }
        }
    }

    private static IWorker getNext() {
        if (workers.isEmpty()) return null;
        if (index >= workers.size()) index = 0;
        return workers.get(index++);
    }

    private static void remove(IWorker worker) {
        workers.remove(worker);
        index--;
        if (index < 0) index = 0;
    }

    public static synchronized void clear() {
        workers.clear();
        index = 0;
        startTime = -1;
    }

    public interface IWorker {
        boolean hasWork();

        /** True to be called again this tick. */
        default boolean doWork() {
            work();
            return false;
        }

        default void work() {
        }
    }
}
