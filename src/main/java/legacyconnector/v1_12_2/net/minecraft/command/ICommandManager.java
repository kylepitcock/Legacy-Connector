package legacyconnector.v1_12_2.net.minecraft.command;

import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** A command dispatcher: the server's (whose commands become modern commands) or the client's. */
public interface ICommandManager {
    int executeCommand(ICommandSender sender, String rawCommand);

    List<String> getTabCompletions(ICommandSender sender, String input, BlockPos pos);

    List<ICommand> getPossibleCommands(ICommandSender sender);

    Map<String, ICommand> getCommands();
}
