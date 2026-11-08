package legacyconnector.v1_20_1.net.minecraftforge.common.util;

/** Forge 1.20.1's {@code NonNullLazy}: a lazily computed, cached, non-null value. */
@FunctionalInterface
public interface NonNullLazy<T> extends NonNullSupplier<T> {
    static <T> NonNullLazy<T> of(NonNullSupplier<T> supplier) {
        Object[] box = new Object[1];
        return () -> {
            synchronized (box) {
                if (box[0] == null) box[0] = supplier.get();
                @SuppressWarnings("unchecked") T v = (T) box[0];
                return v;
            }
        };
    }

    static <T> NonNullLazy<T> concurrentOf(NonNullSupplier<T> supplier) {
        return of(supplier);
    }
}
