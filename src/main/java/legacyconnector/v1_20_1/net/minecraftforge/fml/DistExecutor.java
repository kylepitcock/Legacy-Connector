package legacyconnector.v1_20_1.net.minecraftforge.fml;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Forge 1.20.1's {@code DistExecutor}: run code only on one side. The supplier-of-runnable indirection existed so the client class would not load on a
 * server; NeoForge 1.21.1 dropped the class (plain {@code if (FMLEnvironment.dist == Dist.CLIENT)} does the same), so the "safe" and "unsafe" forms are the same here.
 */
public final class DistExecutor {
    private DistExecutor() {
    }

    public interface SafeRunnable extends Runnable, Serializable {
    }

    public interface SafeCallable<T> extends Callable<T>, Serializable {
    }

    public static void runWhenOn(Dist dist, Supplier<Runnable> toRun) {
        if (dist == FMLEnvironment.dist) toRun.get().run();
    }

    public static void unsafeRunWhenOn(Dist dist, Supplier<Runnable> toRun) {
        runWhenOn(dist, toRun);
    }

    public static void safeRunWhenOn(Dist dist, Supplier<SafeRunnable> toRun) {
        if (dist == FMLEnvironment.dist) toRun.get().run();
    }

    public static <T> T callWhenOn(Dist dist, Supplier<Callable<T>> toRun) {
        if (dist != FMLEnvironment.dist) return null;
        try {
            return toRun.get().call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T unsafeCallWhenOn(Dist dist, Supplier<Callable<T>> toRun) {
        return callWhenOn(dist, toRun);
    }

    public static <T> T safeCallWhenOn(Dist dist, Supplier<SafeCallable<T>> toRun) {
        if (dist != FMLEnvironment.dist) return null;
        try {
            return toRun.get().call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T runForDist(Supplier<Supplier<T>> clientTarget, Supplier<Supplier<T>> serverTarget) {
        return switch (FMLEnvironment.dist) {
            case CLIENT -> clientTarget.get().get();
            case DEDICATED_SERVER -> serverTarget.get().get();
        };
    }

    public static <T> T unsafeRunForDist(Supplier<Supplier<T>> clientTarget, Supplier<Supplier<T>> serverTarget) {
        return runForDist(clientTarget, serverTarget);
    }

    public static <T> T safeRunForDist(Supplier<SafeSupplier<T>> clientTarget, Supplier<SafeSupplier<T>> serverTarget) {
        return switch (FMLEnvironment.dist) {
            case CLIENT -> clientTarget.get().get();
            case DEDICATED_SERVER -> serverTarget.get().get();
        };
    }

    public interface SafeSupplier<T> extends Supplier<T>, Serializable {
    }
}
