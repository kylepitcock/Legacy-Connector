package legacyconnector.v1_12_2.net.minecraftforge.fml.common.event;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.Loader;

public class FMLPostInitializationEvent extends FMLStateEvent {
    @Override
    public legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState getModState() {
        return legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState.POSTINITIALIZED;
    }

    public Object buildSoftDependProxy(String modId, String className) {
        return null;
    }
}
