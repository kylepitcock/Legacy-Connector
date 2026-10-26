package legacyconnector.v1_12_2.net.minecraftforge.fml.common.event;

/** The server is stopping. */
public class FMLServerStoppingEvent extends FMLStateEvent {
    public FMLServerStoppingEvent(Object... data) {
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState getModState() {
        return legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState.AVAILABLE;
    }
}
