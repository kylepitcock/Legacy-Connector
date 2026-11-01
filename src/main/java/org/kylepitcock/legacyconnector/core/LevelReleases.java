package org.kylepitcock.legacyconnector.core;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Releases the legacy views of levels and servers the game is done with ({@code perf.releaseUnloadedLevels}).
 *
 * <p>Legacy views of modern objects ({@code World} of a level, {@code MinecraftServer}, {@code GameRules}, {@code MapStorage},
 * ...) are cached per object in weak-keyed maps, but every view holds its modern object, so an entry never goes away on its own:
 * each client level left behind by a dimension change (with all its chunks, entities and light data, 50-150 MB at render
 * distance 12) and each integrated server of a closed singleplayer world stayed in memory. Here the entries of a level are
 * removed when the level unloads, those of a server when it has stopped (lowest priority: legacy unload handlers have already
 * seen the views).</p>
 *
 * <p>Nothing changes for code that still holds a released view: it is the same object. A view asked for again after its
 * level unloaded (shutdown order: chunk unload events after the level's) is the released one as long as anybody holds it
 * (weak reference), else a fresh one, which nobody can tell apart (views compare by their level).</p>
 */
public final class LevelReleases {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Memory");
    /** Unloaded levels, stopped servers and the objects that died with them; identity, weak. */
    private static final Set<Object> DEAD = java.util.Collections.newSetFromMap(new com.google.common.collect.MapMaker().weakKeys().makeMap());
    /** Released views by their key, weak both ways (identity keys). */
    private static final Map<Object, WeakReference<Object>> RELEASED = new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private static final List<Consumer<LevelAccessor>> LEVEL_ACTIONS = new CopyOnWriteArrayList<>();
    private static final List<Consumer<net.minecraft.server.MinecraftServer>> SERVER_ACTIONS = new CopyOnWriteArrayList<>();
    private static volatile boolean enabled;

    private LevelReleases() {}

    /** Hooks the unload and stop events when {@code perf.releaseUnloadedLevels} is on. */
    public static void register(IEventBus bus) {
        if (!LegacyConfig.perfReleaseUnloadedLevels()) return;
        enabled = true;
        bus.addListener(EventPriority.LOWEST, net.neoforged.neoforge.event.level.LevelEvent.Unload.class, e -> {
            LevelAccessor level = e.getLevel();
            if (level == null) return;
            DEAD.add(level);
            if (level instanceof net.minecraft.server.level.ServerLevel s) DEAD.add(s.getDataStorage());
            for (var a : LEVEL_ACTIONS) run(a, level);
        });
        bus.addListener(EventPriority.LOWEST, net.neoforged.neoforge.event.server.ServerStoppedEvent.class, e -> {
            var server = e.getServer();
            DEAD.add(server);
            for (var a : SERVER_ACTIONS) run(a, server);
        });
    }

    private static <T> void run(Consumer<T> action, T arg) {
        try {
            action.accept(arg);
        } catch (RuntimeException | LinkageError ex) {
            LOG.debug("Releasing the legacy views of {} failed", arg, ex);
        }
    }

    public static boolean enabled() {
        return enabled;
    }

    /** Runs {@code action} for each level that unloads (after the legacy unload handlers). */
    public static void onLevelUnload(Consumer<LevelAccessor> action) {
        LEVEL_ACTIONS.add(action);
    }

    /** Runs {@code action} for each server that has stopped. */
    public static void onServerStopped(Consumer<net.minecraft.server.MinecraftServer> action) {
        SERVER_ACTIONS.add(action);
    }

    /** True for an unloaded level, a stopped server, or an object released with one (identity). */
    public static boolean isDead(Object o) {
        return o != null && enabled && !DEAD.isEmpty() && DEAD.contains(o);
    }

    /** Removes {@code key}'s view from {@code views}; the view stays reachable through {@link #view} while anybody holds it. */
    public static <K, V> void release(Map<K, V> views, K key) {
        if (key == null) return;
        DEAD.add(key);
        V v = views.remove(key);
        if (v != null) RELEASED.put(key, new WeakReference<>(v));
    }

    /**
     * {@code views.computeIfAbsent(key, factory)} for a live key; for a released one the released view while it is held, else a
     * new view that is not cached in {@code views}.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> V view(Map<K, V> views, K key, Function<? super K, ? extends V> factory) {
        if (!isDead(key)) return views.computeIfAbsent(key, factory);
        synchronized (RELEASED) {
            WeakReference<Object> ref = RELEASED.get(key);
            Object v = ref == null ? null : ref.get();
            if (v == null) {
                v = factory.apply(key);
                if (v != null) RELEASED.put(key, new WeakReference<>(v));
            }
            return (V) v;
        }
    }
}
