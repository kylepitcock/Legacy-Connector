package legacyconnector.v1_12_2.net.minecraftforge.fml.common.event;

/** The server has started. */
public class FMLServerStartedEvent extends FMLStateEvent {
    public FMLServerStartedEvent(Object... data) {
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState getModState() {
        return legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState.AVAILABLE;
    }
}
