package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.network.syncher.EntityDataAccessor;

/** Implemented by {@code SynchedEntityData.Builder} (SynchedEntityDataBuilderMixin): replaces the default of an id that is already defined. */
public interface OverridableBuilder {
    <T> void legacyconnector$overrideDefault(EntityDataAccessor<T> accessor, T value);
}
