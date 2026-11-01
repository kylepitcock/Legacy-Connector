package org.kylepitcock.legacyconnector.mixin.perf;

import org.kylepitcock.legacyconnector.core.perf.LegacySlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** perf: one field holding the legacy world view of a level (World.mcc$wrap); see {@link LegacySlot}. Adds state only, changes no behaviour. */
@Mixin(net.minecraft.world.level.Level.class)
public abstract class LevelSlotMixin implements LegacySlot {
    @Unique
    private volatile Object legacyconnector$slot1710;

    @Override
    public Object legacyconnector$slot1710() {
        return legacyconnector$slot1710;
    }

    @Override
    public void legacyconnector$setSlot1710(Object value) {
        legacyconnector$slot1710 = value;
    }
}
