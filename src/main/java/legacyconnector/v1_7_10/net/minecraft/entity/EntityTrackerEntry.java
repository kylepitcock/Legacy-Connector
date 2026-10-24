package legacyconnector.v1_7_10.net.minecraft.entity;

import legacyconnector.v1_7_10.net.minecraft.network.Packet;

/**
 * 1.7.10's per-entity tracking record. Entities are tracked by the modern server; mods that reach into the tracker to widen
 * a tracking range or push a teleport packet (HBM's missiles and bullets) get this stand-in, whose operations do nothing.
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

    public EntityTrackerEntry(Entity entity) {
        this.myEntity = entity;
    }

    /** Sends the packet to every player tracking the entity (and the entity itself when it is a player). */
    public void func_151259_a(Packet packet) {
    }

    public void sendPacketToAllTrackingPlayers(Packet packet) {
    }
}
