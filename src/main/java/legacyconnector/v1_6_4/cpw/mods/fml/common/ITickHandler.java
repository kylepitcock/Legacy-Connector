package legacyconnector.v1_6_4.cpw.mods.fml.common;

import java.util.EnumSet;

/** FML 1.6's tick callback; registered with {@code TickRegistry.registerTickHandler}. */
public interface ITickHandler {
    void tickStart(EnumSet<TickType> type, Object... tickData);

    void tickEnd(EnumSet<TickType> type, Object... tickData);

    /** The tick types this handler wants. */
    EnumSet<TickType> ticks();

    String getLabel();
}
