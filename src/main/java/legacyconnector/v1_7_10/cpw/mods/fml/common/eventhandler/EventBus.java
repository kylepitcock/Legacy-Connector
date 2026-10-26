package legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler;

import java.lang.annotation.Annotation;
import org.kylepitcock.legacyconnector.core.event.ReflectiveEventBus;

public class EventBus implements IEventExceptionHandler {
    private static final ReflectiveEventBus.Adapter ADAPTER = new ReflectiveEventBus.Adapter() {
        @Override
        public Class<? extends Annotation> subscribeAnnotation() {
            return SubscribeEvent.class;
        }

        @Override
        public Class<?> eventBaseClass() {
            return Event.class;
        }

        @Override
        public int priority(Annotation subscribe) {
            return 4 - ((SubscribeEvent) subscribe).priority().ordinal();
        }

        @Override
        public boolean receiveCanceled(Annotation subscribe) {
            return ((SubscribeEvent) subscribe).receiveCanceled();
        }

        @Override
        public boolean isCanceled(Object event) {
            return ((Event) event).isCanceled();
        }
    };

    /** Buses by id, as Forge numbered them for listener lists: FML's bus 0, Forge's main, terrain and ore buses 1 to 3. */
    private static final java.util.Map<Integer, EventBus> BY_ID = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicInteger NEXT_ID = new java.util.concurrent.atomic.AtomicInteger(4);

    private final ReflectiveEventBus delegate;
    private final int busID;

    public EventBus() {
        this("legacy-1.7.10");
    }

    public EventBus(String name) {
        delegate = new ReflectiveEventBus(name, ADAPTER);
        busID = switch (name) {
            case "1.7.10-fml" -> 0;
            case "1.7.10-forge" -> 1;
            case "1.7.10-terrain" -> 2;
            case "1.7.10-oregen" -> 3;
            default -> NEXT_ID.getAndIncrement();
        };
        BY_ID.put(busID, this);
    }

    public void register(Object target) {
        delegate.register(target);
    }

    public void unregister(Object target) {
        delegate.unregister(target);
    }

    /** @return true if the event was canceled */
    public boolean post(Event event) {
        return delegate.post(event);
    }

    /** Handler failures are caught and logged by the bus itself; this is the hook Forge called for them. */
    @Override
    public void handleException(EventBus bus, Event event, IEventListener[] listeners, int index, Throwable throwable) {
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/EventBus").error("Exception caught during firing event {}", event, throwable);
    }

    /** Bridge helper: avoids building legacy event objects nobody listens to. */
    public boolean mcc$hasListeners(Class<? extends Event> type) {
        return delegate.hasListeners(type);
    }

    public int mcc$busId() {
        return busID;
    }

    static EventBus mcc$byId(int id) {
        return BY_ID.get(id);
    }

    java.util.List<ReflectiveEventBus.ListenerView> mcc$listenersFor(Class<?> eventType) {
        return delegate.listenersFor(eventType);
    }
}
