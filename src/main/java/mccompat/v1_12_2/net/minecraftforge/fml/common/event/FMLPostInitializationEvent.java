package mccompat.v1_12_2.net.minecraftforge.fml.common.event;

import mccompat.v1_12_2.net.minecraftforge.fml.common.Loader;

public class FMLPostInitializationEvent extends FMLStateEvent {
    @Override
    public mccompat.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState getModState() {
        return mccompat.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState.POSTINITIALIZED;
    }

    public Object buildSoftDependProxy(String modId, String className) {
        return null;
    }
}
