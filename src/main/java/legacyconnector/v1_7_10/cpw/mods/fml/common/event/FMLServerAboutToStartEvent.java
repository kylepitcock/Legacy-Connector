package legacyconnector.v1_7_10.cpw.mods.fml.common.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.Loader;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;

public class FMLServerAboutToStartEvent extends FMLStateEvent {
    private final MinecraftServer server;

    public FMLServerAboutToStartEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }

    @Override
    public legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.ModState getModState() {
        return legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.ModState.AVAILABLE;
    }
}
