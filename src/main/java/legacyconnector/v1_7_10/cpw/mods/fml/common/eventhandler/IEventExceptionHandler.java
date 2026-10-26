package legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler;

/** Told when a handler throws while an event is posted. */
public interface IEventExceptionHandler {
    void handleException(EventBus bus, Event event, IEventListener[] listeners, int index, Throwable throwable);
}
