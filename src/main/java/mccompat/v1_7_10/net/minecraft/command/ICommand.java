package mccompat.v1_7_10.net.minecraft.command;

import java.util.List;

public interface ICommand extends Comparable {
    String getCommandName();

    String getCommandUsage(ICommandSender sender);

    List getCommandAliases();

    void processCommand(ICommandSender sender, String[] args);

    boolean canCommandSenderUseCommand(ICommandSender sender);

    List addTabCompletionOptions(ICommandSender sender, String[] args);

    boolean isUsernameIndex(String[] args, int index);
}
