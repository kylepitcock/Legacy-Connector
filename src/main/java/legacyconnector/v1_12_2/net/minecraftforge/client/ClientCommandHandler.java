package legacyconnector.v1_12_2.net.minecraftforge.client;

import legacyconnector.v1_12_2.net.minecraft.command.ICommand;

/**
 * Forge's client-side commands ({@code ClientCommandHandler.instance.registerCommand}): run on the client without asking the server. They
 * become the game's client commands (see {@code ClientCommands1122}), with the local player as the sender.
 */
public class ClientCommandHandler extends legacyconnector.v1_12_2.net.minecraft.command.CommandHandler {
    public static final ClientCommandHandler instance = new ClientCommandHandler();

    /** The last completions {@link #autoComplete} found. */
    public String[] latestAutoComplete = null;

    /** Runs a "/..." line if it names a client command; returns 1 when it ran one, 0 otherwise. */
    @Override
    public int executeCommand(legacyconnector.v1_12_2.net.minecraft.command.ICommandSender sender, String message) {
        String line = message.trim();
        if (line.startsWith("/")) line = line.substring(1);
        String[] parts = line.split(" ");
        ICommand command = getCommands().get(parts[0]);
        if (command == null) return 0;
        return super.executeCommand(sender, line);
    }

    /** Completes a chat line starting with "/" against the client commands. */
    public void autoComplete(String leftOfCursor) {
        latestAutoComplete = null;
        if (leftOfCursor == null || !leftOfCursor.startsWith("/")) return;
        var player = legacyconnector.v1_12_2.net.minecraft.client.Minecraft.mcc$get$player(legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft());
        if (player == null) return;
        var completions = getTabCompletions(player, leftOfCursor.substring(1), player.getPosition());
        if (completions != null && !completions.isEmpty()) latestAutoComplete = completions.toArray(new String[0]);
    }
}
