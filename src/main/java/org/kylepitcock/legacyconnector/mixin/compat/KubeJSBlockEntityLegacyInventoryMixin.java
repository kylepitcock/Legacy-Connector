package org.kylepitcock.legacyconnector.mixin.compat;

import legacyconnector.v1_20_1.KubeJSBlockEntityCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/** Restores the KubeJS 6 block-entity inventory convenience methods. */
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
}
