package org.kylepitcock.legacyconnector.compat.jdk;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.WeakHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Behavioural shims for JDK APIs whose semantics changed between Java 8 and Java 21. */
public final class JdkCompat {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/JDK");
    private static final Map<ClassLoader, URLClassLoader> VIEWS = new WeakHashMap<>();

    private JdkCompat() {}

    /**
     * Java 8 code commonly did {@code (URLClassLoader) getClass().getClassLoader()} to read or extend the class
     * path. Modern class loaders are not URLClassLoaders; this returns a URLClassLoader with no URLs that
     * delegates to the real loader, so lookups keep working and {@code getURLs()} is merely empty.
     */
    public static URLClassLoader asURLClassLoader(Object loader) {
        if (loader == null || loader instanceof URLClassLoader) return (URLClassLoader) loader;
        if (!(loader instanceof ClassLoader cl)) throw new ClassCastException(loader.getClass().getName() + " cannot be cast to java.net.URLClassLoader");
        synchronized (VIEWS) {
            return VIEWS.computeIfAbsent(cl, parent -> new URLClassLoader(new URL[0], parent));
        }
    }

    /**
     * Arrays.asList(array).toArray() returned an array of the argument's type up to Java 8 (JDK-6260652, fixed in 9 to Object[]); old code casts it
     * ({@code (String[]) Arrays.asList(names).toArray()}).
     */
    public static <T> java.util.List<T> asList(T[] array) {
        java.util.Objects.requireNonNull(array);
        return new ArrayView<>(array);
    }

    private static final class ArrayView<T> extends java.util.AbstractList<T> implements java.util.RandomAccess {
        private final T[] a;
        ArrayView(T[] a) { this.a = a; }
        @Override public int size() { return a.length; }
        @Override public T get(int i) { return a[i]; }
        @Override public T set(int i, T v) { T old = a[i]; a[i] = v; return old; }
        @Override public Object[] toArray() { return a.clone(); }
        @Override public int indexOf(Object o) { for (int i = 0; i < a.length; i++) if (java.util.Objects.equals(o, a[i])) return i; return -1; }
        @Override public boolean contains(Object o) { return indexOf(o) >= 0; }
        @Override public void sort(java.util.Comparator<? super T> c) { java.util.Arrays.sort(a, c); }
        @Override public void forEach(java.util.function.Consumer<? super T> action) { for (T t : a) action.accept(t); }
    }

    /** Thread.stop() throws since Java 20; interrupting is the closest safe behaviour. */
    public static void stop(Thread thread) {
        LOG.warn("Legacy code called Thread.stop() on {}; interrupting instead", thread.getName());
        thread.interrupt();
    }

    /** Thread.stop(Throwable) is gone since Java 11; interrupting is the closest safe behaviour. */
    public static void stop(Thread thread, Throwable cause) {
        LOG.warn("Legacy code called Thread.stop({}) on {}; interrupting instead", cause, thread.getName());
        thread.interrupt();
    }

    public static void suspend(Thread thread) {
        LOG.warn("Legacy code called Thread.suspend() on {}; ignored", thread.getName());
    }

    public static void resume(Thread thread) {
    }

    /** Installing a SecurityManager throws since Java 18 (JEP 411); legacy mods used it to intercept System.exit. */
    public static void setSecurityManager(SecurityManager manager) {
        LOG.warn("Legacy code tried to install a SecurityManager ({}); ignored", manager == null ? "null" : manager.getClass().getName());
    }

    /** Thrown instead of exiting the JVM when a legacy mod asks to; the mod's loading fails with it. */
    public static RuntimeException exitRequested(int code) {
        return new IllegalStateException("Legacy mod requested the game to exit (code " + code + "); the mod is disabled instead");
    }

    private static final java.util.Set<String> EXIT_CALLERS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * {@code System.exit} from a legacy mod: ignored, and the mod carries on. Mods call it for their own reasons (Counter Craft exits when more than five mods are
     * loaded, an anti-modpack check that no pack satisfies) and the game must not end; unwinding the mod's call instead would abandon its client setup half way.
     */
    public static void exit(int code) {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        String caller = stack.length > 1 ? stack[1].getClassName() + "." + stack[1].getMethodName() : "?";
        if (EXIT_CALLERS.add(caller)) LOG.warn("Legacy code called System.exit({}) from {}; ignored, the mod carries on", code, caller);
    }

    /** {@code result}, or {@code fallback} when the result is null or empty (see the loader's UpdateCheckFix). */
    public static String[] nonEmpty(String[] result, String[] fallback) {
        return result == null || result.length == 0 ? fallback : result;
    }

    /** Inserted by the loader before client-class references a mod probes for under {@code catch (NoClassDefFoundError)} on a dedicated server. */
    public static void clientClassAbsent(String className) {
        throw new NoClassDefFoundError(className.replace('.', '/'));
    }
}
