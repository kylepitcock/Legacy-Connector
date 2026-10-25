package org.kylepitcock.legacyconnector.loader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.transform.LegacyJarTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates the jars that are not cached on several threads (the jars are independent once the classes of all legacy jars have been
 * scanned). The results are handed out in the order the caller asks for them. A jar whose parallel translation fails is translated again
 * on the calling thread, so a thread-safety problem costs time, never a mod. While the caller waits, it keeps the early loading window
 * alive (FML's window only handles events when the main thread ticks it).
 */
final class TranslationPool implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector");
    private static final AtomicInteger THREAD_IDS = new AtomicInteger();

    private final LegacyJarTransformer transformer;
    private final ExecutorService executor;
    private final Map<LegacyJar, Future<LegacyJarTransformer.Result>> futures = new IdentityHashMap<>();

    /** @param threads how many jars to translate at once; 1 or less translates on the calling thread only */
    TranslationPool(LegacyJarTransformer transformer, int threads) {
        this.transformer = transformer;
        this.executor = threads <= 1 ? null : Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "legacyconnector-translate-" + THREAD_IDS.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    static int defaultThreads(int jars) {
        int configured = Integer.getInteger("legacyconnector.translationThreads", 0);
        int threads = configured > 0 ? configured : Math.max(1, Math.min(6, Runtime.getRuntime().availableProcessors() - 2));
        return Math.max(1, Math.min(threads, jars));
    }

    /** Starts translating the jar in the background; {@link #take} returns the result. */
    void submit(LegacyJar jar, Path out, Map<String, ClassInfo> peers) {
        if (executor == null) return;
        futures.put(jar, executor.submit(() -> transformer.transform(jar, out, peers)));
    }

    /** The translation of the jar: waits for the background one, or translates now if there is none or it failed. */
    LegacyJarTransformer.Result take(LegacyJar jar, Path out, Map<String, ClassInfo> peers) throws IOException {
        Future<LegacyJarTransformer.Result> future = futures.remove(jar);
        if (future != null) {
            try {
                while (true) {
                    try {
                        return future.get(40, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException waiting) {
                        heartbeat();
                    }
                }
            } catch (ExecutionException failed) {
                LOG.warn("Legacy Connector: translating {} on a worker thread failed ({}); translating it again on the loading thread", jar.fileName(),
                        String.valueOf(failed.getCause()));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        return transformer.transform(jar, out, peers);
    }

    /**
     * Lets the early loading window handle its events and repaint. It must run on the main thread (GLFW), which is where jars are
     * located; anywhere else, or without a window, it does nothing.
     */
    static void heartbeat() {
        if (!"main".equals(Thread.currentThread().getName())) return;
        try {
            net.neoforged.fml.loading.ImmediateWindowHandler.renderTick();
        } catch (Throwable ignored) {
            // cosmetic: no window, or a provider that does not support ticking
        }
    }

    List<LegacyJar> pending() {
        return List.copyOf(futures.keySet());
    }

    @Override
    public void close() {
        if (executor != null) executor.shutdownNow();
    }
}
