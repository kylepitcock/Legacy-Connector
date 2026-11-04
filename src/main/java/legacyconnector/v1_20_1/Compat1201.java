package legacyconnector.v1_20_1;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;

/** Static replacements for Forge 1.20.1 calls whose meaning changed; the 1.20.1 translator redirects the call sites here ({@code CallRewrites1201}). */
public final class Compat1201 {
    private Compat1201() {
    }

    /** Forge's {@code IEventBus.post} returned true when a listener cancelled the event; NeoForge's returns the event. */
    public static boolean post(IEventBus bus, Event event) {
        bus.post(event);
        return event instanceof ICancellableEvent cancellable && cancellable.isCanceled();
    }
}
