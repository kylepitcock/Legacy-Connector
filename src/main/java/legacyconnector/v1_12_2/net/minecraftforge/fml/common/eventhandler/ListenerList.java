package legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler;

/**
 * Forge 1.12's per-event-class listener list. The layer's event bus dispatches through its own registrations, so this is only the
 * object event classes hand out from {@code getListenerList()}; it holds no listeners.
 */
public class ListenerList {
    public ListenerList() {
    }

    public ListenerList(ListenerList parent) {
    }

    /** The listeners of a bus: none here, the layer's own bus dispatches by registration. */
    public IEventListener[] getListeners(int bus) {
        return new IEventListener[0];
    }

    public void unregisterAll(int id) {
    }

    public static void clearBusID(int id) {
    }
}
