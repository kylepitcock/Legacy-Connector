package legacyconnector.v1_20_1.net.minecraftforge.eventbus.api;

import legacyconnector.v1_20_1.net.minecraftforge.eventbus.ListenerList;
import net.neoforged.bus.api.Event;

/** Forge 1.20.1's lookup of the listener table of an event class (see {@link ListenerList}: detached, always empty). */
public final class EventListenerHelper {
    private static final ListenerList LIST = new ListenerList();

    private EventListenerHelper() {
    }

    public static ListenerList getListenerList(Class<?> eventClass) {
        return LIST;
    }

    public static boolean isEventListened(Class<? extends Event> eventClass) {
        return false;
    }

    public static void clearAll() {
    }
}
