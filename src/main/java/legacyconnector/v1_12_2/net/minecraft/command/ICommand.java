package legacyconnector.v1_12_2.net.minecraft.command;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/**
 * A command. {@code execute} throws the synthesized {@code CommandException} in 1.12, which this interface can only declare as
 * {@link Exception} (checked exceptions are not part of the method's descriptor).
 */
public interface ICommand extends Comparable<ICommand> {
    String getName();

    String getUsage(ICommandSender sender);

    List<String> getAliases();

    void execute(MinecraftServer server, ICommandSender sender, String[] args) throws Exception;

    boolean checkPermission(MinecraftServer server, ICommandSender sender);

    List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos);

    boolean isUsernameIndex(String[] args, int index);
}
