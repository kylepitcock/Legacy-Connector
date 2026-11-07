package legacyconnector.v1_6_4.facade.net.minecraft.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/** Facade of the 1.7.10 shim's {@code EntityTracker} for 1.5.2 / 1.6.4 mods: the two ways to send a packet to the players that see an entity. Never instantiated. */
public final class EntityTracker {
    private EntityTracker() {}

    /** EntityTracker.sendPacketToAllPlayersTrackingEntity(entity, packet): everyone tracking the entity, not the entity itself. */
    public static void sendPacketToAllPlayersTrackingEntity(legacyconnector.v1_7_10.net.minecraft.entity.EntityTracker self, Entity entity, Packet packet) {
        self.sendToAllTrackingEntity(entity, packet);
    }

    /** EntityTracker.sendPacketToAllAssociatedPlayers(entity, packet): the tracking players, and the entity itself when it is a player. */
    public static void sendPacketToAllAssociatedPlayers(legacyconnector.v1_7_10.net.minecraft.entity.EntityTracker self, Entity entity, Packet packet) {
        self.func_151248_b(entity, packet);
    }
}
