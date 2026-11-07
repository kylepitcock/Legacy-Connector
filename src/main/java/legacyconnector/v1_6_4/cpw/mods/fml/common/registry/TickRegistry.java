package legacyconnector.v1_6_4.cpw.mods.fml.common.registry;

import legacyconnector.v1_6_4.cpw.mods.fml.common.IScheduledTickHandler;
import legacyconnector.v1_6_4.cpw.mods.fml.common.ITickHandler;
import legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Ticks164;

/** FML 1.6's tick handler registry; the handlers are driven by {@code bridge.Ticks164} from the 1.7.10 shim's tick events. */
public final class TickRegistry {
    private TickRegistry() {}

    /** Registers {@code handler} for the ticks of {@code side} (a handler of {@code Side.SERVER} runs on the server thread, one of {@code CLIENT} on the client). */
    public static void registerTickHandler(ITickHandler handler, Side side) {
        Ticks164.register(handler, side);
    }

    public static void registerScheduledTickHandler(IScheduledTickHandler handler, Side side) {
        Ticks164.register(handler, side);
    }
}
