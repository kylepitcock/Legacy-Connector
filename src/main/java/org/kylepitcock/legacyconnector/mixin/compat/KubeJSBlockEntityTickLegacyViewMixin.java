package org.kylepitcock.legacyconnector.mixin.compat;

import legacyconnector.v1_20_1.KubeJSBlockEntityCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/**
 * Preserves the useful BlockEntityJS properties/methods on the KubeJS 7
 * BlockEntityTickKubeEvent passed to migrated KubeJS 6 callbacks.
 */
@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.block.entity.BlockEntityTickKubeEvent", remap = false)
public abstract class KubeJSBlockEntityTickLegacyViewMixin {
    @Unique
    public Object getData() {
        return KubeJSBlockEntityCompat.eventData(this);
    }

    @Unique
    public int getX() {
        return KubeJSBlockEntityCompat.eventCoordinate(this, "x");
    }

    @Unique
    public int getY() {
        return KubeJSBlockEntityCompat.eventCoordinate(this, "y");
    }

    @Unique
    public int getZ() {
        return KubeJSBlockEntityCompat.eventCoordinate(this, "z");
    }

    @Unique
    public Object getInventory() {
        return KubeJSBlockEntityCompat.eventInventory(this);
    }

    @Unique
    public Object getPlacer() {
        return KubeJSBlockEntityCompat.eventPlacer(this);
    }

    @Unique
    public Object getPlacerId() {
        return KubeJSBlockEntityCompat.eventPlacerId(this);
    }

    @Unique
    public Object getEntity() {
        return KubeJSBlockEntityCompat.eventBlockEntity(this);
    }

    @Unique
    public void save() {
        KubeJSBlockEntityCompat.eventSave(this);
    }

    @Unique
    public void sync() {
        KubeJSBlockEntityCompat.eventSync(this);
    }

    @Unique
    public void sendEvent(int eventId, int data) {
        KubeJSBlockEntityCompat.eventSendBlockEvent(this, eventId, data);
    }
}
