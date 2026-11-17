package legacyconnector.v1_12_2.net.minecraft.command;

/**
 * The server's command manager. Legacy mods cast {@code server.getCommandManager()} to this class to call {@code registerCommand}
 * (the vanilla type is {@code ServerCommandManager extends CommandHandler}); the server's handler is an instance of it.
 */
public class ServerCommandManager extends CommandHandler {
    public ServerCommandManager() {
        super();
    }
}
