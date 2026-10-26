package legacyconnector.v1_12_2.net.minecraftforge.fml.common.event;

import legacyconnector.v1_12_2.net.minecraft.command.CommandHandler;
import legacyconnector.v1_12_2.net.minecraft.command.ICommand;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;

/** The server is starting: mods register their commands here. */
public class FMLServerStartingEvent extends FMLStateEvent {
    private final MinecraftServer server;

    public FMLServerStartingEvent(Object... data) {
        this.server = data.length > 0 && data[0] instanceof MinecraftServer s ? s : MinecraftServer.mcc$instance();
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState getModState() {
        return legacyconnector.v1_12_2.net.minecraftforge.fml.common.LoaderState.ModState.AVAILABLE;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public void registerServerCommand(ICommand command) {
        ((CommandHandler) server.getCommandManager()).registerCommand(command);
    }
}
