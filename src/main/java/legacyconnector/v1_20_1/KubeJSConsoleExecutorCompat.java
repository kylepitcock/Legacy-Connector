package legacyconnector.v1_20_1;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Keeps KubeJS ConsoleJS log clearing safe during script unload if its
 * background executor has already entered shutdown.
 */
public final class KubeJSConsoleExecutorCompat {
    private KubeJSConsoleExecutorCompat() {}

    /**
     * Submit normally; only if a shut-down ExecutorService rejects the task,
     * execute it on the caller so a log reset cannot abort script reload.
     *
     * @return true when submitted to the executor, false when run inline
     */
    public static boolean executeOrRunAfterShutdown(Executor executor, Runnable task) {
        try {
            executor.execute(task);
            return true;
        } catch (RejectedExecutionException rejected) {
            if (executor instanceof ExecutorService service && service.isShutdown()) {
                task.run();
                return false;
            }
            throw rejected;
        }
    }
}
