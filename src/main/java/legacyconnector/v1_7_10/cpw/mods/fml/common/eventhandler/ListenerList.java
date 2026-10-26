package legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler;

import java.util.ArrayList;
import java.util.List;

/**
 * The listeners of one event class, per bus (Forge's buses are numbered, see {@link EventBus#mcc$busId}). The entries are
 * built from the bus's registrations: each priority followed by its handlers, highest first, as Forge listed them
 * (OpenBlocks dumps the death-drop handlers to find out who takes a player's items).
 */
public class ListenerList {
    private final Class<? extends Event> eventType;

    public ListenerList() {
        this(Event.class);
    }

    public ListenerList(ListenerList parent) {
        this(parent == null ? Event.class : parent.eventType);
    }

    ListenerList(Class<? extends Event> eventType) {
        this.eventType = eventType;
    }

    public IEventListener[] getListeners(int id) {
        EventBus bus = EventBus.mcc$byId(id);
        if (bus == null) return new IEventListener[0];
        List<IEventListener> out = new ArrayList<>();
        EventPriority current = null;
        for (var view : bus.mcc$listenersFor(eventType)) {
            int ordinal = Math.max(0, Math.min(4, 4 - view.priority()));
            EventPriority p = EventPriority.values()[ordinal];
            if (p != current) {
                out.add(p);
                current = p;
            }
            out.add(new ASMEventHandler(view, p));
        }
        return out.toArray(new IEventListener[0]);
    }

    /** Listeners are registered through {@link EventBus#register}; direct additions are not supported. */
    public void register(int id, EventPriority priority, IEventListener listener) {
    }

    public void unregister(int id, IEventListener listener) {
    }

    public static void unregisterAll(int id, IEventListener listener) {
    }
}
