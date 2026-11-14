package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.neoforged.fml.ModList;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Stand-in for Forge 1.20.1's {@code net.minecraftforge.fml.OptionalMod} (gone in FML 4): a value that exists only while a mod is loaded.
 * {@code of(modId)} wraps the mod's container; {@code of(modId, supplier)} calls the supplier lazily, only if the mod is loaded.
 */
public final class LegacyOptionalMod<T> {
    private final String modId;
    private final Supplier<? extends T> supplier;
    private boolean resolved;
    private T value;

    private LegacyOptionalMod(String modId, Supplier<? extends T> supplier) {
        this.modId = modId;
        this.supplier = supplier;
    }

    @SuppressWarnings("unchecked")
    public static <T> LegacyOptionalMod<T> of(String modId) {
        return new LegacyOptionalMod<>(modId, () -> (T) ModList.get().getModContainerById(modId).orElse(null));
    }

    public static <T> LegacyOptionalMod<T> of(String modId, Supplier<? extends T> supplier) {
        return new LegacyOptionalMod<>(modId, supplier);
    }

    public static <T> LegacyOptionalMod<T> empty(String modId) {
        return new LegacyOptionalMod<>(modId, () -> null);
    }

    public String getModId() {
        return modId;
    }

    private T resolve() {
        if (!resolved) {
            ModList list = ModList.get();
            if (list == null) return null; // mod list not built yet: ask again later
            value = list.isLoaded(modId) ? supplier.get() : null;
            resolved = true;
        }
        return value;
    }

    public boolean isPresent() {
        return resolve() != null;
    }

    public T get() {
        T v = resolve();
        if (v == null) throw new NoSuchElementException("Mod " + modId + " is not loaded");
        return v;
    }

    public void ifPresent(Consumer<? super T> action) {
        T v = resolve();
        if (v != null) action.accept(v);
    }

    public <U> LegacyOptionalMod<U> map(Function<? super T, ? extends U> fn) {
        T v = resolve();
        if (v == null) return empty(modId);
        U u = fn.apply(v);
        return new LegacyOptionalMod<>(modId, () -> u);
    }

    public <U> LegacyOptionalMod<U> flatMap(Function<? super T, LegacyOptionalMod<U>> fn) {
        T v = resolve();
        if (v == null) return empty(modId);
        return fn.apply(v);
    }

    public LegacyOptionalMod<T> filter(Predicate<? super T> p) {
        T v = resolve();
        return v != null && p.test(v) ? this : empty(modId);
    }

    public T orElse(T other) {
        T v = resolve();
        return v != null ? v : other;
    }

    public T orElseGet(Supplier<? extends T> other) {
        T v = resolve();
        return v != null ? v : other.get();
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> ex) throws X {
        T v = resolve();
        if (v == null) throw ex.get();
        return v;
    }

    public Optional<T> toOptional() {
        return Optional.ofNullable(resolve());
    }

    @Override
    public String toString() {
        return "OptionalMod[" + modId + (isPresent() ? ", present]" : ", absent]");
    }
}
