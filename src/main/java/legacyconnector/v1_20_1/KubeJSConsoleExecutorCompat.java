package legacyconnector.v1_20_1;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Keeps KubeJS ConsoleJS log tasks safe if its background executor has
 * completely terminated during shutdown.
 */
public final class KubeJSConsoleExecutorCompat {
    private KubeJSConsoleExecutorCompat() {}

    /**
     * Submit normally; only if a terminated ExecutorService rejects the task,
     * execute it on the caller so log reset/flush cannot abort script reload.
     *
     * @return true when submitted to the executor, false when run inline
     */
    public static boolean executeOrRunAfterShutdown(Executor executor, Runnable task) {
        try {
            executor.execute(task);
            return true;
        } catch (RejectedExecutionException rejected) {
            if (executor instanceof ExecutorService service && service.isTerminated()) {
                task.run();
                return false;
            }
            throw rejected;
        }
    }
}
