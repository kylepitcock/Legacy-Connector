package org.kylepitcock.legacyconnector.compat.jdk;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import java.util.concurrent.TimeUnit;

/** Guava APIs that legacy Minecraft shipped (Guava 15-21) but were removed by Guava 32. */
public final class GuavaCompat {
    private GuavaCompat() {}

    public static <T> UnmodifiableIterator<T> emptyIterator() {
        return ImmutableSet.<T>of().iterator();
    }

    public static long elapsedMillis(Stopwatch stopwatch) {
        return stopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    public static long elapsedTime(Stopwatch stopwatch, TimeUnit unit) {
        return stopwatch.elapsed(unit);
    }

    /** Guava 26 dropped the executor-less addCallback; the old one ran the callback directly on the completing thread. */
    public static <V> void addCallback(com.google.common.util.concurrent.ListenableFuture<V> future,
            com.google.common.util.concurrent.FutureCallback<? super V> callback) {
        com.google.common.util.concurrent.Futures.addCallback(future, callback, com.google.common.util.concurrent.MoreExecutors.directExecutor());
    }
}
