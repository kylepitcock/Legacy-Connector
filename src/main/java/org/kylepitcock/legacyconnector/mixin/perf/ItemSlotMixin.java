package org.kylepitcock.legacyconnector.mixin.perf;

import org.kylepitcock.legacyconnector.core.perf.LegacySlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** perf: one field holding the legacy item of a modern item (ItemBridge.legacyOfModern); see {@link LegacySlot}. Adds state only, changes no behaviour. */
@Mixin(net.minecraft.world.item.Item.class)
public abstract class ItemSlotMixin implements LegacySlot {
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
