package legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler;

/** Handler priorities; as in Forge each is also a listener-list entry that marks the start of its phase. */
public enum EventPriority implements IEventListener {
    HIGHEST, HIGH, NORMAL, LOW, LOWEST;

    @Override
    public void invoke(Event event) {
        event.setPhase(this);
    }
}
