package legacyconnector.v1_7_10.cpw.mods.fml.common.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.Loader;

public class FMLServerStoppingEvent extends FMLStateEvent {
    @Override
    public legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.ModState getModState() {
        return legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.ModState.AVAILABLE;
    }
}
