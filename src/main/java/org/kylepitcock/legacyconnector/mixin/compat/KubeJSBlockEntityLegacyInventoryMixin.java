package org.kylepitcock.legacyconnector.mixin.compat;

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

    @Unique
    public void serverTick(Object callback) {
        KubeJSBlockEntityCompat.serverTick(this, callback);
    }

    @Unique
    public void serverTick(int frequency, int offset, Object callback) {
        KubeJSBlockEntityCompat.serverTick(this, frequency, offset, callback);
    }

    @Unique
    public void clientTick(Object callback) {
        KubeJSBlockEntityCompat.clientTick(this, callback);
    }

    @Unique
    public void clientTick(int frequency, int offset, Object callback) {
        KubeJSBlockEntityCompat.clientTick(this, frequency, offset, callback);
    }

    @Unique
    public void tick(Object callback) {
        KubeJSBlockEntityCompat.tick(this, callback);
    }

    @Unique
    public void tick(int frequency, int offset, Object callback) {
        KubeJSBlockEntityCompat.tick(this, frequency, offset, callback);
    }
}
