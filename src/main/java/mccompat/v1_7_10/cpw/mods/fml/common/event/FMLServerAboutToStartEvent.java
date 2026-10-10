package mccompat.v1_7_10.cpw.mods.fml.common.event;

import mccompat.v1_7_10.cpw.mods.fml.common.Loader;
import mccompat.v1_7_10.net.minecraft.server.MinecraftServer;

public class FMLServerAboutToStartEvent extends FMLStateEvent {
    private final MinecraftServer server;

    public FMLServerAboutToStartEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }

    @Override
    public Loader.LoaderState getModState() {
        return Loader.LoaderState.SERVER_ABOUT_TO_START;
    }
}
