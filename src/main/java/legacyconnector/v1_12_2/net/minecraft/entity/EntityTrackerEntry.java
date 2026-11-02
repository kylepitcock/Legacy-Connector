package legacyconnector.v1_12_2.net.minecraft.entity;

import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;

/** 1.12.2 tracking entry of one entity: the players that see it (those watching its chunk, as the modern chunk map tracks it). */
public class EntityTrackerEntry {
    private final Entity trackedEntity;
    @Deprecated
    public Set<EntityPlayerMP> trackingPlayers;

    public EntityTrackerEntry(Entity entity, int range, int maxRange, int updateFrequency, boolean sendVelocityUpdates) {
        this.trackedEntity = entity;
    }

    public EntityTrackerEntry(Entity entity, int range, int updateFrequency, boolean sendVelocityUpdates) {
        this(entity, range, range, updateFrequency, sendVelocityUpdates);
    }

    public static Set<EntityPlayerMP> mcc$get$trackingPlayers(EntityTrackerEntry e) {
        Set<EntityPlayerMP> out = new java.util.HashSet<>();
        var m = e.trackedEntity == null ? null : e.trackedEntity.mcc$entity;
        if (m != null && m.level() instanceof net.minecraft.server.level.ServerLevel level) {
            for (var p : level.getChunkSource().chunkMap.getPlayers(m.chunkPosition(), false)) {
                if (p != m && org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(p) instanceof EntityPlayerMP mp) out.add(mp);
            }
        }
        return out;
    }

    public static void mcc$set$trackingPlayers(EntityTrackerEntry e, Set<EntityPlayerMP> v) {
    }

    /** Ticks since the entry was made (1.12 counted them; mods reset it to force a position resend). */
    public int updateCounter;
    private int mcc$maxRange;

    /** Sends the packet to the players that see the entity. */
    public void sendPacketToTrackedPlayers(legacyconnector.v1_12_2.net.minecraft.network.Packet<?> packet) {
        var m = trackedEntity == null ? null : trackedEntity.mcc$entity;
        var modern = packet == null ? null : packet.mcc$modern();
        if (modern != null && m != null && m.level() instanceof net.minecraft.server.level.ServerLevel level) level.getChunkSource().broadcast(m, modern);
    }

    /** Sends the packet to the players that see the entity, and to the entity when it is a player. */
    public void sendToTrackingAndSelf(legacyconnector.v1_12_2.net.minecraft.network.Packet<?> packet) {
        var m = trackedEntity == null ? null : trackedEntity.mcc$entity;
        var modern = packet == null ? null : packet.mcc$modern();
        if (modern != null && m != null && m.level() instanceof net.minecraft.server.level.ServerLevel level) level.getChunkSource().broadcastAndSend(m, modern);
    }

    /** The modern game decides who sees an entity; the range is only recorded. */
    public void setMaxRange(int maxRange) {
        this.mcc$maxRange = maxRange;
    }

    public Entity getTrackedEntity() {
        return trackedEntity;
    }

    public boolean isVisibleTo(EntityPlayerMP player) {
        return player != null && mcc$get$trackingPlayers(this).contains(player);
    }

    public void updatePlayerEntity(EntityPlayerMP player) {
    }

    public void updatePlayerList(java.util.List<legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer> players) {
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EntityTrackerEntry e && e.trackedEntity == trackedEntity;
    }

    @Override
    public int hashCode() {
        return trackedEntity == null ? 0 : trackedEntity.getEntityId();
    }
}
