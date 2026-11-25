package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;

/** Implemented by {@code SynchedEntityData.Builder} (SynchedEntityDataBuilderMixin): replaces the default of an id that is already defined. */
public interface OverridableBuilder {
    <T> void legacyconnector$overrideDefault(EntityDataAccessor<T> accessor, T value);

    /** Shares the live builder slots without running build's completeness check. */
    SynchedEntityData legacyconnector$view();
}
