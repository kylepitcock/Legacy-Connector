package legacyconnector.v1_12_2.net.minecraft.command;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * Who runs a command: entities, players, the server, command blocks. {@code setCommandStat(CommandResultStats.Type, int)} names a
 * synthesized type and is reached through the synthesized helpers (it does nothing by default, as vanilla's default did).
 */
public interface ICommandSender {
    String getName();

    default ITextComponent getDisplayName() {
        return new TextComponentString(getName());
    }

    default void sendMessage(ITextComponent component) {
    }

    boolean canUseCommand(int permLevel, String commandName);

    default BlockPos getPosition() {
        return BlockPos.ORIGIN;
    }

    default Vec3d getPositionVector() {
        return Vec3d.ZERO;
    }

    World getEntityWorld();

    default Entity getCommandSenderEntity() {
        return null;
    }

    default boolean sendCommandFeedback() {
        return false;
    }

    MinecraftServer getServer();
}
