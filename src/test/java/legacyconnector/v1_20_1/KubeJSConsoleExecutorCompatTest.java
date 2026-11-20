package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

class KubeJSConsoleExecutorCompatTest {
    @Test
    void keepsNormalExecutorSubmissionAsynchronous() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        var taskRan = new java.util.concurrent.CountDownLatch(1);
        try {
            assertTrue(KubeJSConsoleExecutorCompat.executeOrRunAfterShutdown(
                    executor, taskRan::countDown));
            assertTrue(taskRan.await(5, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void runsLogResetInlineOnlyAfterExecutorShutdown() {
        var executor = Executors.newSingleThreadExecutor();
        executor.shutdown();

        var runs = new java.util.concurrent.atomic.AtomicInteger();
        assertFalse(KubeJSConsoleExecutorCompat.executeOrRunAfterShutdown(executor, runs::incrementAndGet));
        assertEquals(1, runs.get());
    }

    @Test
    void preservesRejectionFromAnExecutorThatIsNotShutDown() {
        Executor rejecting = task -> {
            throw new RejectedExecutionException("not a shutdown executor");
        };
        var runs = new java.util.concurrent.atomic.AtomicInteger();

        assertThrows(RejectedExecutionException.class,
                () -> KubeJSConsoleExecutorCompat.executeOrRunAfterShutdown(rejecting, runs::incrementAndGet));
        assertEquals(0, runs.get());
    }

    @Test
    void doesNotRetryWhenTaskItselfFailsOnDirectExecutor() {
        Executor direct = Runnable::run;
        var failure = new RejectedExecutionException("raised by task");

        assertThrows(RejectedExecutionException.class,
                () -> KubeJSConsoleExecutorCompat.executeOrRunAfterShutdown(direct, () -> {
                    throw failure;
                }));
    }
}
