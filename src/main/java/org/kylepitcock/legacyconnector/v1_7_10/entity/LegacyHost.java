package org.kylepitcock.legacyconnector.v1_7_10.entity;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;

/**
 * A modern entity that runs a legacy mod entity. The host forwards the modern entity's hooks (tick, damage, saving,
 * interaction, ...) to the legacy object; the legacy base classes in the shim, reached through {@code super}, call
 * back into the host's vanilla implementations ({@code mcc$super*}) so behaviour composes as it did on 1.7.10.
 */
public interface LegacyHost {
    /** The legacy object (null on a client until its spawn data arrived). */
    Entity mcc$legacy();

    void mcc$superTick();

    void mcc$superBaseTick();

    boolean mcc$superHurt(DamageSource source, float amount);

    void mcc$setSize(float width, float height);

    /** Server: legacy DataWatcher values changed. */
    void mcc$syncWatcher(CompoundTag values);

    default net.minecraft.world.entity.Entity mcc$self() {
        return (net.minecraft.world.entity.Entity) this;
    }
}
