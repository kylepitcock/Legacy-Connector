package legacyconnector.v1_7_10.net.minecraftforge.client;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.command.ICommand;

/** Commands that run on the client only (never sent to the server). Mods register them here; they are kept for the chat bridge. */
public class ClientCommandHandler {
    public static final ClientCommandHandler instance = new ClientCommandHandler();

    private final List<ICommand> commands = new ArrayList<>();

    public ICommand registerCommand(ICommand command) {
        commands.add(command);
        return command;
    }

    public List<ICommand> mcc$commands() {
        return commands;
    }

    public int executeCommand(legacyconnector.v1_7_10.net.minecraft.command.ICommandSender sender, String message) {
        String line = message.startsWith("/") ? message.substring(1) : message;
        String[] parts = line.split(" ");
        if (parts.length == 0) return 0;
        for (ICommand command : commands) {
            if (command.getCommandName().equalsIgnoreCase(parts[0])) {
                command.processCommand(sender, java.util.Arrays.copyOfRange(parts, 1, parts.length));
                return 1;
            }
        }
        return 0;
    }
}
