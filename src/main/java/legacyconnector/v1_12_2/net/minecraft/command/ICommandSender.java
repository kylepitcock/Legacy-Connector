package legacyconnector.v1_12_2.net.minecraft.command;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Who runs a command: entities, players, the server, command blocks. */
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

    /** A command's result for the sender's {@code /stats} targets: nothing by default, as vanilla's default (entities keep their own). */
    default void setCommandStat(CommandResultStats.Type type, int amount) {
    }
}
