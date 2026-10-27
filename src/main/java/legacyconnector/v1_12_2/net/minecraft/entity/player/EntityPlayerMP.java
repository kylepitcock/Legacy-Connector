package legacyconnector.v1_12_2.net.minecraft.entity.player;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 server player: a view over a modern server player. */
public class EntityPlayerMP extends EntityPlayer implements legacyconnector.v1_12_2.net.minecraft.inventory.IContainerListener {
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

    public void setGameType(legacyconnector.v1_12_2.net.minecraft.world.GameType type) {
        var player = mcc$serverPlayer();
        if (player != null && type != null) player.setGameMode(type.mcc$modern());
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

    // the player as a listener of the container it has open (progress bars, forced slot updates)

    @Override
    public void sendAllContents(legacyconnector.v1_12_2.net.minecraft.inventory.Container container,
            legacyconnector.v1_12_2.net.minecraft.util.NonNullList<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> items) {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.GuiBridge1122.sendAllContents(mcc$serverPlayer(), container);
    }

    @Override
    public void sendSlotContents(legacyconnector.v1_12_2.net.minecraft.inventory.Container container, int slot,
            legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack) {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.GuiBridge1122.sendSlotContents(mcc$serverPlayer(), container, slot, stack);
    }

    @Override
    public void sendWindowProperty(legacyconnector.v1_12_2.net.minecraft.inventory.Container container, int id, int value) {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.GuiBridge1122.sendWindowProperty(mcc$serverPlayer(), container, id, value);
    }

    @Override
    public void sendAllWindowProperties(legacyconnector.v1_12_2.net.minecraft.inventory.Container container,
            legacyconnector.v1_12_2.net.minecraft.inventory.IInventory inventory) {
        for (int i = 0; i < inventory.getFieldCount(); ++i) sendWindowProperty(container, i, inventory.getField(i));
    }

    // ------------------------------------------------------------------ server player state (area E, 1.12.2)

    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.server.management.PlayerInteractionManager interactionManager;
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.entity.Entity spectatingEntity;
    @Deprecated
    public String language;
    private legacyconnector.v1_12_2.net.minecraft.server.management.PlayerInteractionManager mcc$interactionManager;

    public static legacyconnector.v1_12_2.net.minecraft.server.management.PlayerInteractionManager mcc$get$interactionManager(EntityPlayerMP p) {
        if (p.mcc$interactionManager == null) {
            p.mcc$interactionManager = legacyconnector.v1_12_2.net.minecraft.server.management.PlayerInteractionManager.mcc$of(p);
        }
        return p.mcc$interactionManager;
    }

    public static void mcc$set$interactionManager(EntityPlayerMP p, legacyconnector.v1_12_2.net.minecraft.server.management.PlayerInteractionManager v) {
        if (v != null) {
            v.player = p;
            p.mcc$interactionManager = v;
        }
    }

    public static legacyconnector.v1_12_2.net.minecraft.entity.Entity mcc$get$spectatingEntity(EntityPlayerMP p) {
        return p.getSpectatingEntity();
    }

    public static void mcc$set$spectatingEntity(EntityPlayerMP p, legacyconnector.v1_12_2.net.minecraft.entity.Entity v) {
        p.setSpectatingEntity(v);
    }

    public static String mcc$get$language(EntityPlayerMP p) {
        var sp = p.mcc$serverPlayer();
        return sp == null ? "en_us" : sp.clientInformation().language();
    }

    public static void mcc$set$language(EntityPlayerMP p, String v) {
        // reported by the client
    }

    public legacyconnector.v1_12_2.net.minecraft.world.WorldServer getServerWorld() {
        return (legacyconnector.v1_12_2.net.minecraft.world.WorldServer) org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(mcc$serverPlayer().serverLevel());
    }

    /** Resends the whole container (slots, carried stack and properties) to the client. */
    public void sendContainerToPlayer(legacyconnector.v1_12_2.net.minecraft.inventory.Container container) {
        var menu = org.kylepitcock.legacyconnector.v1_12_2.entity.MenuView1122.modern(container);
        if (menu != null) menu.sendAllDataToRemote();
        else if (container != null) mcc$serverPlayer().containerMenu.sendAllDataToRemote();
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.Entity getSpectatingEntity() {
        var camera = mcc$serverPlayer().getCamera();
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(camera);
    }

    public void setSpectatingEntity(legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        mcc$serverPlayer().setCamera(entity == null ? null : entity.mcc$entity);
    }

    public void markPlayerActive() {
        mcc$serverPlayer().resetLastActionTime();
    }

    public boolean hasSpawnDimension() {
        return true;
    }

    /** Forge: moves the player to another dimension; the teleporter places it (or the same position when it is a vanilla teleporter). */
    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.Entity changeDimension(int dimension,
            legacyconnector.v1_12_2.net.minecraftforge.common.util.ITeleporter teleporter) {
        return super.changeDimension(dimension, teleporter);
    }
}
