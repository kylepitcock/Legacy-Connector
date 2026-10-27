package legacyconnector.v1_12_2.net.minecraft.entity.player;

import legacyconnector.v1_12_2.net.minecraft.world.World;

public class EntityPlayerMP extends EntityPlayer {
    /** Phantom field: accesses are redirected to {@code mcc$get$connection}. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer connection;
    private legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer mcc$connection;

    public EntityPlayerMP(World world) {
        super(world);
    }

    public static legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer mcc$get$connection(EntityPlayerMP p) {
        if (p.mcc$serverPlayer() == null) return null;
        if (p.mcc$connection == null) p.mcc$connection = new legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer(p);
        return p.mcc$connection;
    }

    public int getPing() {
        var player = mcc$serverPlayer();
        return player == null || player.connection == null ? 0 : player.connection.latency();
    }

    public void setGameType(legacyconnector.v1_12_2.net.minecraft.world.GameType type) {
        var player = mcc$serverPlayer();
        if (player != null && type != null) player.setGameMode(type.mcc$modern());
    }

    /** Moves the player to another dimension through the modern teleport (no portal is placed, as Forge's teleporter-less call). */
    public void changeDimension(int dimension) {
        var player = mcc$serverPlayer();
        if (player == null) return;
        var level = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.levelOf(player.server, dimension);
        if (level != null) player.teleportTo(level, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    public net.minecraft.server.level.ServerPlayer mcc$serverPlayer() {
        return (net.minecraft.server.level.ServerPlayer) mcc$entity;
    }

    /** Operators of the needed level may use a command (single player's host and cheats-enabled worlds included). */
    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        var player = mcc$serverPlayer();
        return player != null && player.hasPermissions(permLevel);
    }

    @Override
    public void sendMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent component) {
        var player = mcc$serverPlayer();
        if (player != null && component != null) player.sendSystemMessage(component.mcc$modern());
    }
}
