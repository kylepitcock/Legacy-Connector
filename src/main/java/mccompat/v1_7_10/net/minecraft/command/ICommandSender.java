package mccompat.v1_7_10.net.minecraft.command;

import mccompat.v1_7_10.net.minecraft.util.ChunkCoordinates;
import mccompat.v1_7_10.net.minecraft.util.IChatComponent;
import mccompat.v1_7_10.net.minecraft.world.World;

public interface ICommandSender {
    String getCommandSenderName();

    IChatComponent getFormattedCommandSenderName();

    void addChatMessage(IChatComponent message);

    boolean canCommandSenderUseCommand(int permissionLevel, String command);

    ChunkCoordinates getPlayerCoordinates();

    World getEntityWorld();
}
