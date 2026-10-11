package mccompat.v1_7_10.cpw.mods.fml.common.event;

import mccompat.v1_7_10.cpw.mods.fml.common.FMLCommonHandler;
import mccompat.v1_7_10.cpw.mods.fml.common.Loader;
import mccompat.v1_7_10.cpw.mods.fml.relauncher.Side;

public abstract class FMLStateEvent extends FMLEvent {
    public abstract mccompat.v1_7_10.cpw.mods.fml.common.LoaderState.ModState getModState();

    public Side getSide() {
        return FMLCommonHandler.instance().getSide();
    }
}
