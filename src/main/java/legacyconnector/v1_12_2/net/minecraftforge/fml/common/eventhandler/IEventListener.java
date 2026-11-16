package legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler;

/** Forge 1.12's handle on one registered event listener. */
public interface IEventListener {
    void invoke(Event event);
}
