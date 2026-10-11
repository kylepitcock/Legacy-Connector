package mccompat.v1_7_10.cpw.mods.fml.common.event;

import mccompat.v1_7_10.cpw.mods.fml.common.Loader;

public class FMLLoadCompleteEvent extends FMLStateEvent {
    @Override
    public mccompat.v1_7_10.cpw.mods.fml.common.LoaderState.ModState getModState() {
        return mccompat.v1_7_10.cpw.mods.fml.common.LoaderState.ModState.AVAILABLE;
    }
}
