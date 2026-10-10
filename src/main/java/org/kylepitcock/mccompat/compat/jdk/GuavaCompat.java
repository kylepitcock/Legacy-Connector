package org.kylepitcock.mccompat.compat.jdk;

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
}
