package mccompat.v1_7_10.cpw.mods.fml.common.event;

import mccompat.v1_7_10.cpw.mods.fml.common.Loader;

public class FMLPostInitializationEvent extends FMLStateEvent {
    @Override
    public Loader.LoaderState getModState() {
        return Loader.LoaderState.POSTINITIALIZATION;
    }

    public Object buildSoftDependProxy(String modId, String className) {
        return null;
    }
}
