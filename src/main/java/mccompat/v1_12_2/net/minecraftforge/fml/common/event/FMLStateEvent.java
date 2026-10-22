package mccompat.v1_12_2.net.minecraftforge.fml.common.event;

import mccompat.v1_12_2.net.minecraftforge.fml.common.FMLCommonHandler;
import mccompat.v1_12_2.net.minecraftforge.fml.common.Loader;
import mccompat.v1_12_2.net.minecraftforge.fml.relauncher.Side;

public abstract class FMLStateEvent extends FMLEvent {
    public abstract mccompat.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState getModState();

    public Side getSide() {
        return FMLCommonHandler.instance().getSide();
    }
}
