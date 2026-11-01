package org.kylepitcock.legacyconnector.core.perf;

/**
 * The 1.7.10 metadata of a modern vanilla block state, remembered on the state ({@code mixin.perf.BlockStateSlotMixin}): plus one,
 * 0 while unknown. The answer depends only on the state (the vanilla flattening table), so it never changes once computed.
 */
public interface LegacyStateSlot {
    int legacyconnector$meta1710();

    void legacyconnector$setMeta1710(int metaPlusOne);
}
