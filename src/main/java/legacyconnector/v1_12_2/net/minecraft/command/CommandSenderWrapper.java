package legacyconnector.v1_12_2.net.minecraft.command;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A command sender with some of its properties replaced (position, entity, permission level, feedback), as functions run commands. */
public class CommandSenderWrapper implements ICommandSender {
    private final ICommandSender delegate;
    private final Vec3d positionVector;
    private final BlockPos position;
    private final Integer permissionLevel;
    private final Entity entity;
    private final Boolean sendCommandFeedback;

    public CommandSenderWrapper(ICommandSender delegateIn, Vec3d positionVectorIn, BlockPos positionIn, Integer permissionLevelIn, Entity entityIn,
            Boolean sendCommandFeedbackIn) {
        this.delegate = delegateIn;
        this.positionVector = positionVectorIn;
        this.position = positionIn;
        this.permissionLevel = permissionLevelIn;
        this.entity = entityIn;
        this.sendCommandFeedback = sendCommandFeedbackIn;
    }

    public static CommandSenderWrapper create(ICommandSender sender) {
        return sender instanceof CommandSenderWrapper w ? w : new CommandSenderWrapper(sender, null, null, null, null, null);
    }

    public CommandSenderWrapper withEntity(Entity entityIn, Vec3d pos) {
        if (entity == entityIn && java.util.Objects.equals(positionVector, pos)) return this;
        return new CommandSenderWrapper(delegate, pos, new BlockPos(pos), permissionLevel, entityIn, sendCommandFeedback);
    }

    public CommandSenderWrapper withPermissionLevel(int level) {
        if (permissionLevel != null && permissionLevel <= level) return this;
        return new CommandSenderWrapper(delegate, positionVector, position, level, entity, sendCommandFeedback);
    }

    public CommandSenderWrapper withSendCommandFeedback(boolean sendCommandFeedbackIn) {
        if (sendCommandFeedback != null && (!sendCommandFeedback || sendCommandFeedbackIn)) return this;
        return new CommandSenderWrapper(delegate, positionVector, position, permissionLevel, entity, sendCommandFeedbackIn);
    }

    public CommandSenderWrapper computePositionVector() {
        return positionVector != null ? this
                : new CommandSenderWrapper(delegate, getPositionVector(), getPosition(), permissionLevel, entity, sendCommandFeedback);
    }

    @Override
    public String getName() {
        return entity != null ? entity.getName() : delegate.getName();
    }

    @Override
    public ITextComponent getDisplayName() {
        return entity != null ? entity.getDisplayName() : delegate.getDisplayName();
    }

    @Override
    public void sendMessage(ITextComponent component) {
        if (sendCommandFeedback == null || sendCommandFeedback) delegate.sendMessage(component);
    }

    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        return (permissionLevel == null || permissionLevel >= permLevel) && delegate.canUseCommand(permLevel, commandName);
    }

    @Override
    public BlockPos getPosition() {
        if (position != null) return position;
        return entity != null ? entity.getPosition() : delegate.getPosition();
    }

    @Override
    public Vec3d getPositionVector() {
        if (positionVector != null) return positionVector;
        return entity != null ? entity.getPositionVector() : delegate.getPositionVector();
    }

    @Override
    public World getEntityWorld() {
        return entity != null ? entity.getEntityWorld() : delegate.getEntityWorld();
    }

    @Override
    public Entity getCommandSenderEntity() {
        return entity != null ? entity.getCommandSenderEntity() : delegate.getCommandSenderEntity();
    }

    @Override
    public boolean sendCommandFeedback() {
        return sendCommandFeedback != null ? sendCommandFeedback : delegate.sendCommandFeedback();
    }

    @Override
    public void setCommandStat(CommandResultStats.Type type, int amount) {
        if (entity != null) entity.setCommandStat(type, amount);
        else delegate.setCommandStat(type, amount);
    }

    @Override
    public MinecraftServer getServer() {
        return delegate.getServer();
    }
}
