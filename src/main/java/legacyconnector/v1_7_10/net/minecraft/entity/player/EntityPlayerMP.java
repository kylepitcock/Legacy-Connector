package legacyconnector.v1_7_10.net.minecraft.entity.player;

import legacyconnector.v1_7_10.net.minecraft.server.MinecraftServer;
import legacyconnector.v1_7_10.net.minecraft.util.IChatComponent;
import legacyconnector.v1_7_10.net.minecraft.world.WorldServer;
import net.minecraft.server.level.ServerPlayer;

public class EntityPlayerMP extends EntityPlayer implements legacyconnector.v1_7_10.net.minecraft.inventory.ICrafting {
    public final MinecraftServer mcServer;
    public int ping;
    private legacyconnector.v1_7_10.net.minecraft.server.management.ItemInWorldManager mcc$manager;

    /** Phantom: the block interaction manager (redirected to {@code mcc$get$theItemInWorldManager}). */
    @Deprecated
    public legacyconnector.v1_7_10.net.minecraft.server.management.ItemInWorldManager theItemInWorldManager;

    public static legacyconnector.v1_7_10.net.minecraft.server.management.ItemInWorldManager mcc$get$theItemInWorldManager(EntityPlayerMP p) {
        if (p.mcc$manager == null) {
            p.mcc$manager = new legacyconnector.v1_7_10.net.minecraft.server.management.ItemInWorldManager(
                    legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(p.mcc$serverPlayer().serverLevel()), p.mcc$serverPlayer());
        }
        return p.mcc$manager;
    }

    private legacyconnector.v1_7_10.net.minecraft.stats.StatisticsFile mcc$stats;

    /** The player's statistics and achievements (its achievements are the player's own, see {@code addStat}). */
    public legacyconnector.v1_7_10.net.minecraft.stats.StatisticsFile getStatFile() {
        if (mcc$stats == null) mcc$stats = new legacyconnector.v1_7_10.net.minecraft.stats.StatisticsFile(mcc$serverPlayer().getUUID());
        return mcc$stats;
    }

    public EntityPlayerMP(ServerPlayer modern) {
        super(modern);
        this.mcServer = MinecraftServer.mcc$wrap(modern.server);
    }

    /**
     * A server player no client is connected to, as mods built with {@code new EntityPlayerMP(server, world, profile,
     * new ItemInWorldManager(world))} (Thaumcraft reading offline research): backed by NeoForge's fake player.
     */
    public EntityPlayerMP(MinecraftServer server, WorldServer world, com.mojang.authlib.GameProfile profile,
            legacyconnector.v1_7_10.net.minecraft.server.management.ItemInWorldManager manager) {
        this(net.neoforged.neoforge.common.util.FakePlayerFactory.get((net.minecraft.server.level.ServerLevel) world.mcc$fullLevel(), profile));
    }

    public ServerPlayer mcc$serverPlayer() {
        return (ServerPlayer) mcc$entity;
    }

    @Override
    public void addChatMessage(IChatComponent message) {
        mcc$serverPlayer().sendSystemMessage(message.mcc$toModern());
    }

    public WorldServer getServerForPlayer() {
        return (WorldServer) WorldServer.mcc$wrap(mcc$serverPlayer().serverLevel());
    }

    public String getPlayerIP() {
        return mcc$serverPlayer().getIpAddress();
    }

    public void setPlayerHealthUpdated() {
    }

    public void mountEntityAndWakeUp() {
        mcc$serverPlayer().stopSleeping();
    }

    public void closeContainer() {
        mcc$serverPlayer().closeContainer();
    }

    public void sendContainerToPlayer(Object container) {
        mcc$serverPlayer().containerMenu.broadcastChanges();
    }

    // ICrafting: mods cast the entries of Container.crafters to EntityPlayerMP, so the player is its own container listener.
    @Override
    public void sendContainerAndContentsToPlayer(legacyconnector.v1_7_10.net.minecraft.inventory.Container container, java.util.List items) {
    }

    @Override
    public void sendSlotContents(legacyconnector.v1_7_10.net.minecraft.inventory.Container container, int slot, legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack) {
    }

    @Override
    public void sendProgressBarUpdate(legacyconnector.v1_7_10.net.minecraft.inventory.Container container, int id, int value) {
        if (mcc$entity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            org.kylepitcock.legacyconnector.v1_7_10.bridge.GuiBridge.sendProgressBar(sp, container.windowId, id, value);
        }
    }

    public void sendContainerToPlayer(legacyconnector.v1_7_10.net.minecraft.inventory.Container container) {
        mcc$serverPlayer().containerMenu.broadcastChanges();
    }

    public void updateHeldItem() {
        mcc$serverPlayer().containerMenu.broadcastChanges();
    }

    public java.util.List loadedChunks = new java.util.ArrayList();

    public void getNextWindowId() {
    }
}
