package legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler;

import org.kylepitcock.legacyconnector.core.event.ReflectiveEventBus;

/** A {@code @SubscribeEvent} method as an entry of an event's listener list (Forge generated a class per handler). */
public class ASMEventHandler implements IEventListener {
    private final ReflectiveEventBus.ListenerView view;
    private final EventPriority priority;

    ASMEventHandler(ReflectiveEventBus.ListenerView view, EventPriority priority) {
        this.view = view;
        this.priority = priority;
    }

    @Override
    public void invoke(Event event) {
        view.invoker().accept(event);
    }

    public EventPriority getPriority() {
        return priority;
    }

    /** The object whose method this is (a {@link Class} for static handlers). */
    public Object mcc$owner() {
        return view.owner();
    }

    @Override
    public String toString() {
        return "ASM: " + view.owner() + " " + view.name();
    }
}
