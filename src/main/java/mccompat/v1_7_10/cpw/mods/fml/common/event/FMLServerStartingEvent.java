package mccompat.v1_7_10.cpw.mods.fml.common.event;

import mccompat.v1_7_10.cpw.mods.fml.common.Loader;
import mccompat.v1_7_10.net.minecraft.command.ICommand;
import mccompat.v1_7_10.net.minecraft.server.MinecraftServer;
import org.kylepitcock.mccompat.v1_7_10.bridge.CommandBridge1710;

public class FMLServerStartingEvent extends FMLStateEvent {
    private final MinecraftServer server;

    public FMLServerStartingEvent(MinecraftServer server) {
        this.server = server;
        mccompat.v1_7_10.net.minecraft.command.CommandHandler.mcc$flushPending(server.mcc$modern());
    }

    public MinecraftServer getServer() {
        return server;
    }

    /** Legacy ICommand -> Brigadier literal with a greedy string argument, see {@link CommandBridge1710}. */
    public void registerServerCommand(ICommand command) {
        CommandBridge1710.register(server.mcc$modern(), command);
    }

    @Override
    public mccompat.v1_7_10.cpw.mods.fml.common.LoaderState.ModState getModState() {
        return mccompat.v1_7_10.cpw.mods.fml.common.LoaderState.ModState.AVAILABLE;
    }
}
