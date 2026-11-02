package legacyconnector.v1_12_2.net.minecraft.entity;

import java.util.HashSet;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.network.Packet;
import legacyconnector.v1_12_2.net.minecraft.world.WorldServer;

/**
 * 1.12.2 entity tracker of a server world: who sees which entity, and sending packets to them. The tracking itself is the modern chunk
 * map's (players watching the entity's chunk); packets go out as the modern packets they describe.
 */
public class EntityTracker {
    private final WorldServer world;
    @Deprecated
    public Set<EntityTrackerEntry> entries;

    public EntityTracker(WorldServer world) {
        this.world = world;
    }

    private net.minecraft.server.level.ServerLevel mcc$level() {
        return world == null ? null : world.mcc$serverLevel();
    }

    public static Set<EntityTrackerEntry> mcc$get$entries(EntityTracker t) {
        Set<EntityTrackerEntry> out = new HashSet<>();
        var level = t.mcc$level();
        if (level != null) {
            for (var e : level.getAllEntities()) {
                var view = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(e);
                if (view != null) out.add(new EntityTrackerEntry(view, 64, 1, true));
            }
        }
        return out;
    }

    /** Phantom: redirected to {@link #mcc$get$trackedEntityHashTable} (tracker entries by entity id, one per entity while it stays). */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.util.IntHashMap<EntityTrackerEntry> trackedEntityHashTable;
    private final java.util.Map<Integer, EntityTrackerEntry> mcc$entryById = new java.util.concurrent.ConcurrentHashMap<>();

    public static legacyconnector.v1_12_2.net.minecraft.util.IntHashMap<EntityTrackerEntry> mcc$get$trackedEntityHashTable(EntityTracker t) {
        return new legacyconnector.v1_12_2.net.minecraft.util.IntHashMap<EntityTrackerEntry>() {
            @Override
            public EntityTrackerEntry lookup(int id) {
                var level = t.mcc$level();
                var e = level == null ? null : level.getEntity(id);
                if (e == null) {
                    t.mcc$entryById.remove(id);
                    return null;
                }
                var view = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(e);
                if (view == null) return null;
                EntityTrackerEntry entry = t.mcc$entryById.get(id);
                if (entry == null || entry.getTrackedEntity() != view) {
                    entry = new EntityTrackerEntry(view, 64, 1, true);
                    t.mcc$entryById.put(id, entry);
                }
                return entry;
            }

            @Override
            public boolean containsItem(int id) {
                return lookup(id) != null;
            }
        };
    }

    public static void mcc$set$entries(EntityTracker t, Set<EntityTrackerEntry> v) {
    }

    /** The players that see the entity (those watching its chunk). */
    public Set<? extends EntityPlayer> getTrackingPlayers(Entity entity) {
        Set<EntityPlayer> out = new HashSet<>();
        var level = mcc$level();
        if (level == null || entity == null || entity.mcc$entity == null) return out;
        var players = level.getChunkSource().chunkMap.getPlayers(entity.mcc$entity.chunkPosition(), false);
        for (var p : players) {
            if (p != entity.mcc$entity && org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(p) instanceof EntityPlayer ep) out.add(ep);
        }
        return out;
    }

    public void sendToTracking(Entity entity, Packet<?> packet) {
        var level = mcc$level();
        var modern = packet == null ? null : packet.mcc$modern();
        if (level != null && modern != null && entity != null && entity.mcc$entity != null) level.getChunkSource().broadcast(entity.mcc$entity, modern);
    }

    public void sendToTrackingAndSelf(Entity entity, Packet<?> packet) {
        var level = mcc$level();
        var modern = packet == null ? null : packet.mcc$modern();
        if (level != null && modern != null && entity != null && entity.mcc$entity != null) level.getChunkSource().broadcastAndSend(entity.mcc$entity, modern);
    }

    /** Tracking is the modern game's; these 1.12 hooks have nothing to do. */
    public void track(Entity entity) {
    }

    public void track(Entity entity, int trackingRange, int updateFrequency) {
    }

    public void track(Entity entity, int trackingRange, int updateFrequency, boolean sendVelocityUpdates) {
    }

    public void untrack(Entity entity) {
    }

    public void tick() {
    }

    public void updateVisibility(EntityPlayerMP player) {
    }

    public void removePlayerFromTrackers(EntityPlayerMP player) {
    }

    public void sendLeashedEntitiesInChunk(EntityPlayerMP player, legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk chunk) {
    }

    public static long getPositionLong(double value) {
        return net.minecraft.util.Mth.lfloor(value * 4096.0D);
    }
}
