package org.kylepitcock.legacyconnector.v1_12_2.entity;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;

/**
 * A modern entity that runs a 1.12.2 mod entity. The host forwards its modern hooks (tick, damage, saving, interaction, ...) to the
 * legacy object; the shim's legacy base classes, reached through {@code super}, call back into the host's vanilla implementations
 * ({@code mcc$super*}) so behaviour composes as it did on 1.12.2.
 */
public interface Host1122 {
    /** The legacy object (null on a client until its spawn data arrived). */
    Entity mcc$legacy();

    void mcc$superTick();

    void mcc$superBaseTick();

    boolean mcc$superHurt(DamageSource source, float amount);

    void mcc$setSize(float width, float height);

    /** Vanilla landing (causeFallDamage, with the multiplier and source of the fall being processed). */
    void mcc$superFall(float distance, float multiplier);

    /** Vanilla handling of being below the world (onBelowWorld). */
    void mcc$superOutOfWorld();

    /** Vanilla lightning hit (thunderHit). */
    void mcc$superThunderHit(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.LightningBolt bolt);

    /** Vanilla placement of a passenger (positionRider). */
    void mcc$superPositionRider(net.minecraft.world.entity.Entity passenger);

    /** Vanilla tick while riding (rideTick). */
    void mcc$superRideTick();

    /** Vanilla client position interpolation (lerpTo). */
    void mcc$superLerpTo(double x, double y, double z, float yaw, float pitch, int steps);

    /** Vanilla handling of an entity event byte (handleEntityEvent). */
    void mcc$superHandleEntityEvent(byte id);

    default net.minecraft.world.entity.Entity mcc$self() {
        return (net.minecraft.world.entity.Entity) this;
    }
}
