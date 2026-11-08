package legacyconnector.v1_20_1.net.minecraftforge.eventbus;

import legacyconnector.v1_20_1.net.minecraftforge.eventbus.api.IEventListener;
import net.neoforged.bus.api.EventPriority;

/**
 * Forge 1.20.1's per-event listener table. NeoForge's bus keeps no such list a mod can reach, so this one is a detached table: asking for it works,
 * registering into it is remembered by nobody, and it has no listeners (mods that look it up to count or attach listeners keep running without effect).
 */
public class ListenerList {
    private static final IEventListener[] NONE = new IEventListener[0];

    public ListenerList() {
    }

    public ListenerList(ListenerList parent) {
    }

    public static int allocateBusID() {
        return 0;
    }

    public IEventListener[] getListeners(int busId) {
        return NONE;
    }

    public void register(int busId, EventPriority priority, IEventListener listener) {
    }

    public void unregister(int busId, IEventListener listener) {
    }

    public static void unregisterAll(int busId, IEventListener listener) {
    }

    public void clearBusID(int busId) {
    }
}
