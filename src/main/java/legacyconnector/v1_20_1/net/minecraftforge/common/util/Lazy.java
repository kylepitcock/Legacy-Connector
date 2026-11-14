package legacyconnector.v1_20_1.net.minecraftforge.common.util;

import java.util.function.Supplier;

/**
 * Forge 1.20.1's {@code Lazy}: an interface (so mods write lambdas and implement it), unlike NeoForge's final class. A lambda is a plain, non-caching
 * supplier exactly as in Forge; {@link #of} and {@link #concurrentOf} cache the first value.
 */
@FunctionalInterface
public interface Lazy<T> extends Supplier<T> {
    @Override
    T get();

    static <T> Lazy<T> of(Supplier<T> supplier) {
        return new Lazy<>() {
            private Supplier<T> src = supplier;
            private T value;

            @Override
            public T get() {
                Supplier<T> s = src;
                if (s != null) {
                    value = s.get();
                    src = null;
                }
                return value;
            }
        };
    }

    static <T> Lazy<T> concurrentOf(Supplier<T> supplier) {
        return new Lazy<>() {
            private volatile Supplier<T> src = supplier;
            private volatile T value;

            @Override
            public T get() {
                if (src != null) {
                    synchronized (this) {
                        Supplier<T> s = src;
                        if (s != null) {
                            value = s.get();
                            src = null;
                        }
                    }
                }
                return value;
            }
        };
    }
}
