package legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler;

/** One entry of an event's listener list: a handler, or a priority marker (which sets the event's phase). */
public interface IEventListener {
    void invoke(Event event);
}
