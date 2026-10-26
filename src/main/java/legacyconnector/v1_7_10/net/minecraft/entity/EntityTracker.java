package legacyconnector.v1_7_10.net.minecraft.entity;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.util.IntHashMap;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;

/**
 * A world's entity tracker. The modern server tracks entities itself (the chunk map knows which players see an entity), so this
 * answers who tracks an entity and sends legacy packets to them; starting or updating tracking is the modern server's job.
 */
public class EntityTracker {
    public final World mcc$world;
    public IntHashMap trackedEntityIDs;
    public java.util.Set trackedEntities = new java.util.HashSet<>();

    public EntityTracker(World world) {
        this.mcc$world = world;
        final Map<Integer, EntityTrackerEntry> entries = new HashMap<>();
        this.trackedEntityIDs = new IntHashMap() {
            @Override
            public Object lookup(int id) {
                Entity entity = mcc$world.getEntityByID(id);
                return entity == null ? null : entries.computeIfAbsent(id, k -> new EntityTrackerEntry(entity));
            }
        };
    }

    /** The players the modern server currently sends this entity to (not the entity itself). */
    public static Set<EntityPlayerMP> mcc$trackingPlayers(Entity entity) {
        Set<EntityPlayerMP> out = new LinkedHashSet<>();
        if (entity == null || entity.mcc$entity == null || !(entity.mcc$entity.level() instanceof ServerLevel level)) return out;
        for (ServerPlayer p : level.getChunkSource().chunkMap.getPlayersWatching(entity.mcc$entity)) {
            if (EntityBridge.wrap(p) instanceof EntityPlayerMP mp) out.add(mp);
        }
        return out;
    }

    /** Sends a legacy packet to one player (custom channels, tile entity updates and vanilla packets the shim knows). */
    static void mcc$send(EntityPlayerMP player, Packet packet) {
        if (player != null && packet != null) new legacyconnector.v1_7_10.net.minecraft.network.NetHandlerPlayServer(player).sendPacket(packet);
    }

    /** Forge: the players tracking the entity. */
    public Set<EntityPlayer> getTrackingPlayers(Entity entity) {
        return new LinkedHashSet<>(mcc$trackingPlayers(entity));
    }

    /** Sends the packet to every player tracking the entity. */
    public void sendToAllTrackingEntity(Entity entity, Packet packet) {
        for (EntityPlayerMP p : mcc$trackingPlayers(entity)) mcc$send(p, packet);
    }

    /** Sends the packet to every player tracking the entity, and to the entity when it is a player. */
    public void func_151248_b(Entity entity, Packet packet) {
        sendToAllTrackingEntity(entity, packet);
        if (entity instanceof EntityPlayerMP self) mcc$send(self, packet);
    }

    /** Runs one tracking update; the modern server updates tracking every tick by itself. */
    public void updateTrackedEntities() {
    }

    public void addEntityToTracker(Entity entity) {
    }

    public void addEntityToTracker(Entity entity, int range, int frequency) {
    }

    public void addEntityToTracker(Entity entity, int range, int frequency, boolean velocityUpdates) {
    }

    public void removeEntityFromAllTrackingPlayers(Entity entity) {
    }

    public void removePlayerFromTrackers(EntityPlayerMP player) {
    }
}
