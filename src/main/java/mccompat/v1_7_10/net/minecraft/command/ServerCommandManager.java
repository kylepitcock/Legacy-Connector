package mccompat.v1_7_10.net.minecraft.command;

public class ServerCommandManager extends CommandHandler {
    public ServerCommandManager() {
        super();
    }

    public ServerCommandManager(net.minecraft.server.MinecraftServer server) {
        super(server);
    }
}
