package legacyconnector.v1_7_10.net.minecraft.entity;

import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/**
 * 1.7.10's per-entity tracking record. Entities are tracked by the modern server: {@code trackingPlayers} are the players it
 * sends the entity to and the packet methods reach them; widening the range or forcing a player to start watching is left to
 * the modern tracker (HBM's missiles and bullets set the fields, which change nothing).
 */
public class EntityTrackerEntry {
    public Entity myEntity;
    public int blocksDistanceThreshold = 80;
    public int updateFrequency = 3;
    public int lastScaledXPosition;
    public int lastScaledYPosition;
    public int lastScaledZPosition;
    public int lastYaw;
    public int lastPitch;
    public int lastHeadMotion;
    public int ticks;
    /** Phantom: the players tracking the entity, a snapshot (redirected to {@code mcc$get$trackingPlayers}). */
    @Deprecated
    public Set trackingPlayers;

    public EntityTrackerEntry(Entity entity) {
        this.myEntity = entity;
    }

    public static Set mcc$get$trackingPlayers(EntityTrackerEntry e) {
        return EntityTracker.mcc$trackingPlayers(e.myEntity);
    }

    public static void mcc$set$trackingPlayers(EntityTrackerEntry e, Set players) {
    }

    /** Sends the packet to every player tracking the entity. */
    public void func_151259_a(Packet packet) {
        for (EntityPlayerMP p : EntityTracker.mcc$trackingPlayers(myEntity)) EntityTracker.mcc$send(p, packet);
    }

    /** Sends the packet to every player tracking the entity, and to the entity when it is a player. */
    public void func_151261_b(Packet packet) {
        func_151259_a(packet);
        if (myEntity instanceof EntityPlayerMP self) EntityTracker.mcc$send(self, packet);
    }

    public void sendPacketToAllTrackingPlayers(Packet packet) {
        func_151259_a(packet);
    }

    /** Starts sending the entity to the player if in range; the modern tracker does this every tick by itself. */
    public void tryStartWachingThis(EntityPlayerMP player) {
    }

    public void removeFromWatchingList(EntityPlayerMP player) {
    }

    public void removePlayerFromTracker(EntityPlayerMP player) {
    }

    public void informAllAssociatedPlayersOfItemDestruction() {
    }
}
