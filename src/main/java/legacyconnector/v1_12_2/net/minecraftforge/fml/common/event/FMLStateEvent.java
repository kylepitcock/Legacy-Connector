package legacyconnector.v1_12_2.net.minecraftforge.fml.common.event;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.FMLCommonHandler;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.Loader;
import legacyconnector.v1_12_2.net.minecraftforge.fml.relauncher.Side;

public abstract class FMLStateEvent extends FMLEvent {
    public abstract legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState getModState();

    public Side getSide() {
        return FMLCommonHandler.instance().getSide();
    }
}
