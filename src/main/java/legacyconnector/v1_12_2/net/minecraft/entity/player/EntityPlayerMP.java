package legacyconnector.v1_12_2.net.minecraft.entity.player;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 server player: a view over a modern server player. */
public class EntityPlayerMP extends EntityPlayer {
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer connection;
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer server;
    @Deprecated
    public int currentWindowId;
    @Deprecated
    public boolean isChangingQuantityOnly;
    @Deprecated
    public boolean invulnerableDimensionChange;
    @Deprecated
    public int ping;
    private legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer mcc$connection;

    public EntityPlayerMP(World world) {
        super(world, null);
    }

    public EntityPlayerMP(net.minecraft.server.level.ServerPlayer modern) {
        super(modern);
    }

    public net.minecraft.server.level.ServerPlayer mcc$serverPlayer() {
        return (net.minecraft.server.level.ServerPlayer) mcc$entity;
    }

    public static legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer mcc$get$connection(EntityPlayerMP p) {
        if (p.mcc$connection == null) p.mcc$connection = new legacyconnector.v1_12_2.net.minecraft.network.NetHandlerPlayServer(p);
        return p.mcc$connection;
    }

    public static legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer mcc$get$server(EntityPlayerMP p) {
        return legacyconnector.v1_12_2.net.minecraft.server.MinecraftServer.mcc$instance();
    }

    public static int mcc$get$currentWindowId(EntityPlayerMP p) {
        var player = p.mcc$serverPlayer();
        return player == null ? 0 : player.containerMenu.containerId;
    }

    public static boolean mcc$get$invulnerableDimensionChange(EntityPlayerMP p) {
        var player = p.mcc$serverPlayer();
        return player != null && player.isChangingDimension();
    }

    public static void mcc$set$invulnerableDimensionChange(EntityPlayerMP p, boolean v) {
        // the modern flag is managed by the teleport itself
    }

    public static int mcc$get$ping(EntityPlayerMP p) {
        var player = p.mcc$serverPlayer();
        return player == null || player.connection == null ? 0 : player.connection.latency();
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

    @Override
    public boolean isSpectator() {
        return mcc$serverPlayer().isSpectator();
    }

    @Override
    public boolean isCreative() {
        return mcc$serverPlayer().isCreative();
    }

    public String getPlayerIP() {
        return mcc$serverPlayer().getIpAddress();
    }

    public boolean hasDisconnected() {
        return mcc$serverPlayer().hasDisconnected();
    }

    public void closeContainer() {
        mcc$serverPlayer().doCloseContainer();
    }

    public void getNextWindowId() {
        // the modern player numbers its containers when it opens them
    }

    public void updateHeldItem() {
        var player = mcc$serverPlayer();
        player.containerMenu.broadcastChanges();
    }

    public void setPlayerHealthUpdated() {
        mcc$serverPlayer().resetSentInfo();
    }

    public void mountEntityAndWakeUp() {
        var player = mcc$serverPlayer();
        player.ejectPassengers();
        if (player.isSleeping()) player.stopSleepInBed(true, false);
    }

    @Override
    public void setPositionAndUpdate(double x, double y, double z) {
        var player = mcc$serverPlayer();
        player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
    }

    public int getPing() {
        return mcc$get$ping(this);
    }
}
