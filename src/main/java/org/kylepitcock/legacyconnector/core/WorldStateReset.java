package org.kylepitcock.legacyconnector.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The one place where static state of the layer that belongs to a single world is dropped when that world is left (issue 113: a
 * single-player session that quits to the title and opens another world keeps the JVM, and with it every static field).
 *
 * <p>A class holding world-dependent statics (registry holders of datapack registries, ids derived from a server's dimensions, per-tick
 * scratch of a stopped server, ...) registers a {@link Runnable} here once, typically from its static initializer; it runs after a server has
 * stopped (the integrated one of a closed single-player world included) and, on a client, when the player logs out. Statics keyed by a
 * level or server that already have a {@link LevelReleases} hook do not need this. The audit that finds candidates is
 * {@code tools/dev/static_state_audit.py} (docs/wiki/Development.md, "State across world loads").</p>
 *
 * <p>The runnables run on the thread that fires the event (the server thread for a stop, the client thread for a logout): state kept in
 * {@link ThreadLocal}s of other threads must be invalidated with a generation counter, not by clearing the local.</p>
 */
public final class WorldStateReset {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/WorldState");
    private static final List<Runnable> ON_SERVER_STOPPED = new CopyOnWriteArrayList<>();
    private static final List<Runnable> ON_CLIENT_LOGOUT = new CopyOnWriteArrayList<>();

    private WorldStateReset() {}

    /** Runs {@code action} after every server stop (dedicated or integrated). */
    public static void onServerStopped(Runnable action) {
        ON_SERVER_STOPPED.add(action);
    }

    /** Runs {@code action} when the client leaves its world (logout of the local player; also fires for the integrated server's own player). */
    public static void onClientLogout(Runnable action) {
        ON_CLIENT_LOGOUT.add(action);
    }

    /** Both: a static that is filled on either side. */
    public static void onWorldLeft(Runnable action) {
        onServerStopped(action);
        onClientLogout(action);
    }

    /** Hooks the stop and logout events. */
    public static void register(IEventBus game) {
        game.addListener(EventPriority.LOWEST, net.neoforged.neoforge.event.server.ServerStoppedEvent.class, e -> runAll(ON_SERVER_STOPPED));
        if (FMLEnvironment.dist.isClient()) ClientHook.register(game);
    }

    /** Runs the server-stop actions (the event handler's body; public for tests). */
    public static void serverStopped() {
        runAll(ON_SERVER_STOPPED);
    }

    /** Runs the logout actions (the event handler's body; public for tests). */
    public static void clientLoggedOut() {
        runAll(ON_CLIENT_LOGOUT);
    }

    private static void runAll(List<Runnable> actions) {
        for (Runnable a : actions) {
            try {
                a.run();
            } catch (RuntimeException | LinkageError ex) {
                LOG.debug("Resetting world state failed", ex);
            }
        }
    }

    /** Client-only event classes stay out of this class's linkage on a server. */
    private static final class ClientHook {
        static void register(IEventBus game) {
            game.addListener(EventPriority.LOWEST, net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut.class, e -> runAll(ON_CLIENT_LOGOUT));
        }
    }
}
