package legacyconnector.v1_7_10.net.minecraft.command;

import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;
import legacyconnector.v1_7_10.net.minecraft.world.World;

public interface ICommandSender {
    String getCommandSenderName();

    IChatComponent getFormattedCommandSenderName();

    void addChatMessage(IChatComponent message);

    boolean canCommandSenderUseCommand(int permissionLevel, String command);

    ChunkCoordinates getPlayerCoordinates();

    World getEntityWorld();
}
