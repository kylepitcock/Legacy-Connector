package legacyconnector.v1_7_10.cpw.mods.fml.common.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.Loader;
import legacyconnector.v1_7_10.net.minecraft.command.ICommand;
import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.CommandBridge1710;

public class FMLServerStartingEvent extends FMLStateEvent {
    private final MinecraftServer server;

    public FMLServerStartingEvent(MinecraftServer server) {
        this.server = server;
        legacyconnector.v1_7_10.net.minecraft.command.CommandHandler.mcc$flushPending(server.mcc$modern());
    }

    public MinecraftServer getServer() {
        return server;
    }

    /** Legacy ICommand -> Brigadier literal with a greedy string argument, see {@link CommandBridge1710}. */
    public void registerServerCommand(ICommand command) {
        CommandBridge1710.register(server.mcc$modern(), command);
    }

    @Override
    public legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.ModState getModState() {
        return legacyconnector.v1_7_10.cpw.mods.fml.common.LoaderState.ModState.AVAILABLE;
    }
}
