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

    /** Guava 26 removed {@code CheckedFuture} and {@code Futures.immediateCheckedFuture}; callers use the result as a plain future (BuildCraft's chunk cache). */
    public static <V> com.google.common.util.concurrent.ListenableFuture<V> immediateCheckedFuture(V value) {
        return com.google.common.util.concurrent.Futures.immediateFuture(value);
    }

    /** Guava 26 dropped the executor-less transform; the old one ran the function on the completing thread. */
    public static <I, O> com.google.common.util.concurrent.ListenableFuture<O> transform(com.google.common.util.concurrent.ListenableFuture<I> future,
            com.google.common.base.Function<? super I, ? extends O> function) {
        return com.google.common.util.concurrent.Futures.transform(future, function, com.google.common.util.concurrent.MoreExecutors.directExecutor());
    }

    public static <I, O> com.google.common.util.concurrent.ListenableFuture<O> transformAsync(com.google.common.util.concurrent.ListenableFuture<I> future,
            com.google.common.util.concurrent.AsyncFunction<? super I, ? extends O> function) {
        return com.google.common.util.concurrent.Futures.transformAsync(future, function, com.google.common.util.concurrent.MoreExecutors.directExecutor());
    }

    private static java.util.List<java.io.File> children(java.io.File dir) {
        java.io.File[] files = dir.isDirectory() ? dir.listFiles() : null;
        return files == null ? java.util.List.of() : java.util.Arrays.asList(files);
    }

    /** Guava's {@code Files.fileTreeTraverser()} (removed in Guava 33), as the 1.12.2 shim's TreeTraverser. */
    public static legacyconnector.v1_12_2.com.google.common.collect.TreeTraverser<java.io.File> fileTreeTraverser1122() {
        return new legacyconnector.v1_12_2.com.google.common.collect.TreeTraverser<>() {
            @Override
            public Iterable<java.io.File> children(java.io.File root) {
                return GuavaCompat.children(root);
            }
        };
    }

    /** The same for the 1.7.10 shim (and the versions linked to it). */
    public static legacyconnector.v1_7_10.com.google.common.collect.TreeTraverser<java.io.File> fileTreeTraverser1710() {
        return new legacyconnector.v1_7_10.com.google.common.collect.TreeTraverser<>() {
            @Override
            public Iterable<java.io.File> children(java.io.File root) {
                return GuavaCompat.children(root);
            }
        };
    }

    private static final java.util.Map<ClassLoader, com.google.common.reflect.ClassPath> CLASS_PATHS = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * {@code ClassPath.from(loader)} as Guava 15 behaved on LaunchWrapper's URLClassLoader: Guava only scans URLClassLoaders (and
     * the system loader's java.class.path), and the module class loaders mods run in are neither, so Logistics Pipes found no
     * packets and every network handler call hit a null. A URLClassLoader over the legacy mod jars is put in front of the loader
     * (class loading still goes to the real loader first), so the scan sees every translated mod jar.
     */
    public static com.google.common.reflect.ClassPath classPathFrom(ClassLoader loader) throws java.io.IOException {
        if (loader == null || loader instanceof java.net.URLClassLoader) return com.google.common.reflect.ClassPath.from(loader);
        com.google.common.reflect.ClassPath cached = CLASS_PATHS.get(loader);
        if (cached != null) return cached;
        java.util.List<java.net.URL> urls = new java.util.ArrayList<>();
        for (org.kylepitcock.legacyconnector.core.LegacyModRecord mod : org.kylepitcock.legacyconnector.core.LegacyMods.ordered()) {
            try {
                java.nio.file.Path file = mod.sourceFile();
                if (file != null && java.nio.file.Files.exists(file)) {
                    java.net.URL url = file.toUri().toURL();
                    if (!urls.contains(url)) urls.add(url);
                }
            } catch (RuntimeException e) {
                // a mod without a readable source file contributes nothing
            }
        }
        com.google.common.reflect.ClassPath result = com.google.common.reflect.ClassPath.from(
                new java.net.URLClassLoader(urls.toArray(new java.net.URL[0]), loader));
        com.mojang.logging.LogUtils.getLogger().info("[LegacyConnector/ClassPath] Guava ClassPath.from(module loader) scans {} legacy mod jar(s): {} top level classes", urls.size(), result.getTopLevelClasses().size());
        CLASS_PATHS.put(loader, result);
        return result;
    }
}
