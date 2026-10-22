package mccompat.v1_12_2.net.minecraftforge.fml.common.event;

import mccompat.v1_12_2.net.minecraftforge.fml.common.Loader;

public class FMLInitializationEvent extends FMLStateEvent {
    @Override
    public mccompat.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState getModState() {
        return mccompat.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState.INITIALIZED;
    }

}
