package legacyconnector.v1_7_10.net.minecraft.command;

import java.util.List;
import java.util.Map;

public interface ICommandManager {
    int executeCommand(ICommandSender sender, String command);

    List getPossibleCommands(ICommandSender sender, String prefix);

    List getPossibleCommands(ICommandSender sender);

    Map getCommands();
}
