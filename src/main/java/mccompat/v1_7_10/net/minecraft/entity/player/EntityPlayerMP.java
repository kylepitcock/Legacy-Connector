package mccompat.v1_7_10.net.minecraft.entity.player;

import mccompat.v1_7_10.net.minecraft.server.MinecraftServer;
import mccompat.v1_7_10.net.minecraft.util.IChatComponent;
import mccompat.v1_7_10.net.minecraft.world.WorldServer;
import net.minecraft.server.level.ServerPlayer;

public class EntityPlayerMP extends EntityPlayer {
    public final MinecraftServer mcServer;
    public int ping;

    public EntityPlayerMP(ServerPlayer modern) {
        super(modern);
        this.mcServer = MinecraftServer.mcc$wrap(modern.server);
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
