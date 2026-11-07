package legacyconnector.v1_6_4.cpw.mods.fml.common;

/** A tick handler that is called only every {@link #nextTickSpacing()} game ticks. */
public interface IScheduledTickHandler extends ITickHandler {
    int nextTickSpacing();
}
