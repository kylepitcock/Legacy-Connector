package legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler;

import java.lang.annotation.Annotation;
import org.kylepitcock.legacyconnector.core.event.ReflectiveEventBus;

public class EventBus {
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

    private final ReflectiveEventBus delegate;

    public EventBus() {
        this("legacy-1.7.10");
    }

    public EventBus(String name) {
        delegate = new ReflectiveEventBus(name, ADAPTER);
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

    /** Bridge helper: avoids building legacy event objects nobody listens to. */
    public boolean mcc$hasListeners(Class<? extends Event> type) {
        return delegate.hasListeners(type);
    }
}
