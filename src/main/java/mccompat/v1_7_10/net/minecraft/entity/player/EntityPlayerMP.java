package mccompat.v1_7_10.net.minecraft.entity.player;

import mccompat.v1_7_10.net.minecraft.server.MinecraftServer;
import mccompat.v1_7_10.net.minecraft.util.IChatComponent;
import mccompat.v1_7_10.net.minecraft.world.WorldServer;
import net.minecraft.server.level.ServerPlayer;

public class EntityPlayerMP extends EntityPlayer {
    public final MinecraftServer mcServer;
    public int ping;
    private mccompat.v1_7_10.net.minecraft.server.management.ItemInWorldManager mcc$manager;

    /** Phantom: the block interaction manager (redirected to {@code mcc$get$theItemInWorldManager}). */
    @Deprecated
    public mccompat.v1_7_10.net.minecraft.server.management.ItemInWorldManager theItemInWorldManager;

    public static mccompat.v1_7_10.net.minecraft.server.management.ItemInWorldManager mcc$get$theItemInWorldManager(EntityPlayerMP p) {
        if (p.mcc$manager == null) {
            p.mcc$manager = new mccompat.v1_7_10.net.minecraft.server.management.ItemInWorldManager(
                    mccompat.v1_7_10.net.minecraft.world.World.mcc$wrap(p.mcc$serverPlayer().serverLevel()), p.mcc$serverPlayer());
        }
        return p.mcc$manager;
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
            mccompat.v1_7_10.net.minecraft.server.management.ItemInWorldManager manager) {
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
}
