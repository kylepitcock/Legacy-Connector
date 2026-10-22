package legacyconnector.v1_12_2.net.minecraftforge.fml.common.event;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.Loader;

public class FMLInitializationEvent extends FMLStateEvent {
    @Override
    public legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState getModState() {
        return legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState.INITIALIZED;
    }

}
