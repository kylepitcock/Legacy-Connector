package legacyconnector.v1_12_2.net.minecraftforge.fml.common.event;

import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;

/** The server is about to load its worlds. */
public class FMLServerAboutToStartEvent extends FMLStateEvent {
    private final MinecraftServer server;

    public FMLServerAboutToStartEvent(Object... data) {
        this.server = data.length > 0 && data[0] instanceof MinecraftServer s ? s : MinecraftServer.mcc$instance();
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState getModState() {
        return legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState.AVAILABLE;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
