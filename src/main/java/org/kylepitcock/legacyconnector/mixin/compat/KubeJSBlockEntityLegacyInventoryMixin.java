package org.kylepitcock.legacyconnector.mixin.compat;

import dev.latvian.mods.rhino.Context;
import legacyconnector.v1_20_1.KubeJSBlockEntityCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/** Restores the KubeJS 6 block-entity inventory and callback-ticking methods. */
@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.block.entity.BlockEntityInfo", remap = false)
public abstract class KubeJSBlockEntityLegacyInventoryMixin {
    @Unique
    public void inventory(int width, int height) {
        KubeJSBlockEntityCompat.inventory(this, width, height);
    }

    @Unique
    public void rightClickOpensInventory() {
        KubeJSBlockEntityCompat.rightClickOpensInventory(this);
    }

    // Rhino recognizes a leading Context parameter, hides it from JavaScript arguments,
    // and injects the active script context when this compatibility method is invoked.
    @Unique
    public void serverTick(Context context, Object callback) {
        KubeJSBlockEntityCompat.serverTick(context, this, callback);
    }

    @Unique
    public void serverTick(Context context, int frequency, int offset, Object callback) {
        KubeJSBlockEntityCompat.serverTick(context, this, frequency, offset, callback);
    }

    @Unique
    public void clientTick(Context context, Object callback) {
        KubeJSBlockEntityCompat.clientTick(context, this, callback);
    }

    @Unique
    public void clientTick(Context context, int frequency, int offset, Object callback) {
        KubeJSBlockEntityCompat.clientTick(context, this, frequency, offset, callback);
    }

    @Unique
    public void tick(Context context, Object callback) {
        KubeJSBlockEntityCompat.tick(context, this, callback);
    }

    @Unique
    public void tick(Context context, int frequency, int offset, Object callback) {
        KubeJSBlockEntityCompat.tick(context, this, frequency, offset, callback);
    }
}
