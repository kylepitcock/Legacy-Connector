package org.kylepitcock.legacyconnector.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * One lock per legacy mod. 1.7.10 ran world generation, packet handlers and commands on the server thread, so mods keep
 * the state they share between them in plain fields (CustomOreGen's per-world caches and wireframe buffers). Modern
 * generation runs on worker threads: a mod's generators, its server-side packet handlers and its commands hold its lock
 * so they do not run at the same time.
 *
 * <p>The server thread may hold a mod's lock while it waits for a chunk that a worker generates with that mod's generator
 * (a command reading a far block): a worker that finds the lock held by a server thread that keeps waiting runs unlocked
 * instead of waiting for ever.
 */
public final class LegacyModLocks {
    private static final Map<Object, OwnedLock> LOCKS = new ConcurrentHashMap<>();

    private LegacyModLocks() {}

    private static final class OwnedLock extends ReentrantLock {
        Thread holder() {
            return getOwner();
        }
    }

    /** The lock of the mod that {@code type} belongs to: its module (each legacy mod jar is a module of its own). */
    public static ReentrantLock of(Class<?> type) {
        Module module = type.getModule();
        Object key = module.isNamed() ? module : type.getClassLoader() != null ? type.getClassLoader() : LegacyModLocks.class;
        return LOCKS.computeIfAbsent(key, k -> new OwnedLock());
    }

    /** The lock of the mod whose code {@code object} is. */
    public static ReentrantLock of(Object object) {
        return of(object.getClass());
    }

    /** Runs server-thread code of a mod under its lock. */
    public static void serverSide(ReentrantLock lock, Runnable body) {
        if (lock == null) {
            body.run();
            return;
        }
        lock.lock();
        try {
            body.run();
        } finally {
            lock.unlock();
        }
    }

    /** Runs a mod's world generator (on a generation worker) under its lock, unless the server thread keeps it. */
    public static void generation(ReentrantLock lock, Runnable body) {
        boolean locked = false;
        try {
            int parked = 0;
            while (true) {
                if (lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                    locked = true;
                    break;
                }
                // a server thread that holds the lock and keeps waiting (parked: for a chunk, perhaps this one) is not
                // going to release it; one that is running (building CustomOreGen's wireframes) will
                Thread holder = ((OwnedLock) lock).holder();
                boolean waiting = holder != null && isServerThread(holder) && holder.getState() != Thread.State.RUNNABLE;
                parked = waiting ? parked + 1 : 0;
                if (parked >= 4) break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            body.run();
        } finally {
            if (locked) lock.unlock();
        }
    }

    private static boolean isServerThread(Thread thread) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server != null && server.getRunningThread() == thread;
    }
}
