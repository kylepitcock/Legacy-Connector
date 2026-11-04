package legacyconnector.v1_20_1.net.minecraftforge.common.util;

/** Forge 1.20.1's non-null functional interface (removed in NeoForge 1.21.1). */
@FunctionalInterface
public interface NonNullFunction<T, R> {
    R apply(T t);
}
