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

    /** Thread.stop() throws since Java 20; interrupting is the closest safe behaviour. */
    public static void stop(Thread thread) {
        LOG.warn("Legacy code called Thread.stop() on {}; interrupting instead", thread.getName());
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

    public static void exit(int code) {
        throw exitRequested(code);
    }
}
