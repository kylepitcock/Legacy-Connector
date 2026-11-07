package legacyconnector.v1_20_1.net.minecraftforge.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Forge 1.20.1's {@code LazyOptional}: a possibly empty, lazily resolved and invalidatable handle on a capability instance. NeoForge 1.21.1 returns
 * the instance (or null) directly; this class keeps the old call shape ({@code getCapability(...).ifPresent(...)}) working over such instances.
 */
public class LazyOptional<T> {
    private static final LazyOptional<Void> EMPTY = new LazyOptional<>(null);

    private final NonNullSupplier<T> supplier;
    private final Object lock = new Object();
    private T resolved;
    private boolean isValid = true;
    private List<NonNullConsumer<LazyOptional<T>>> listeners;

    private LazyOptional(NonNullSupplier<T> instanceSupplier) {
        this.supplier = instanceSupplier;
    }

    @SuppressWarnings("unchecked")
    public static <T> LazyOptional<T> empty() {
        return (LazyOptional<T>) EMPTY;
    }

    public static <T> LazyOptional<T> of(NonNullSupplier<T> instanceSupplier) {
        return instanceSupplier == null ? empty() : new LazyOptional<>(instanceSupplier);
    }

    /** A handle on an instance that may be null (NeoForge's capability lookups return null for "none"). */
    public static <T> LazyOptional<T> ofNullable(T instance) {
        return instance == null ? empty() : new LazyOptional<>(() -> instance);
    }

    private T getValue() {
        if (!isValid || supplier == null) return null;
        if (resolved == null) {
            synchronized (lock) {
                if (resolved == null) resolved = supplier.get();
            }
        }
        return resolved;
    }

    public boolean isPresent() {
        return supplier != null && isValid;
    }

    public void ifPresent(NonNullConsumer<? super T> consumer) {
        T v = getValue();
        if (v != null) consumer.accept(v);
    }

    public <U> LazyOptional<U> lazyMap(NonNullFunction<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent()) return empty();
        LazyOptional<U> ret = of(() -> mapper.apply(getValue()));
        addListener(self -> ret.invalidate());
        return ret;
    }

    public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        T v = getValue();
        return v == null ? Optional.empty() : Optional.ofNullable(mapper.apply(v));
    }

    public <U> Optional<U> flatMap(Function<? super T, Optional<U>> mapper) {
        Objects.requireNonNull(mapper);
        T v = getValue();
        return v == null ? Optional.empty() : mapper.apply(v);
    }

    public Optional<T> filter(Predicate<? super T> predicate) {
        T v = getValue();
        return v != null && predicate.test(v) ? Optional.of(v) : Optional.empty();
    }

    public <U> LazyOptional<U> cast() {
        @SuppressWarnings("unchecked")
        LazyOptional<U> self = (LazyOptional<U>) this;
        return self;
    }

    public T orElse(T other) {
        T v = getValue();
        return v != null ? v : other;
    }

    public T orElseGet(Supplier<? extends T> other) {
        T v = getValue();
        return v != null ? v : other.get();
    }

    public <X extends Throwable> T orElseThrow(NonNullSupplier<? extends X> exceptionSupplier) throws X {
        return orElseThrow((Supplier<? extends X>) exceptionSupplier::get);
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        T v = getValue();
        if (v != null) return v;
        throw exceptionSupplier.get();
    }

    public Optional<T> resolve() {
        return Optional.ofNullable(getValue());
    }

    public Stream<T> stream() {
        T v = getValue();
        return v == null ? Stream.empty() : Stream.of(v);
    }

    public void addListener(NonNullConsumer<LazyOptional<T>> listener) {
        if (isPresent()) {
            if (listeners == null) listeners = new ArrayList<>();
            listeners.add(listener);
        } else {
            listener.accept(this);
        }
    }

    public void invalidate() {
        if (isValid && supplier != null) {
            isValid = false;
            if (listeners != null) {
                List<NonNullConsumer<LazyOptional<T>>> copy = listeners;
                listeners = null;
                copy.forEach(l -> l.accept(this));
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LazyOptional<?> other)) return false;
        return Objects.equals(getValue(), other.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }
}
