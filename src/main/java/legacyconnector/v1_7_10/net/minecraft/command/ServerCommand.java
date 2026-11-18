package legacyconnector.v1_7_10.net.minecraft.command;

/** A console command line waiting in the dedicated server's queue. */
public class ServerCommand {
    public final String command;
    public final ICommandSender sender;

    public ServerCommand(String command, ICommandSender sender) {
        this.command = command;
        this.sender = sender;
    }
}
