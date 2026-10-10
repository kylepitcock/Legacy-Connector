package mccompat.v1_7_10.cpw.mods.fml.common.event;

import mccompat.v1_7_10.cpw.mods.fml.common.Loader;

public class FMLServerStartedEvent extends FMLStateEvent {
    @Override
    public Loader.LoaderState getModState() {
        return Loader.LoaderState.SERVER_STARTED;
    }
}
