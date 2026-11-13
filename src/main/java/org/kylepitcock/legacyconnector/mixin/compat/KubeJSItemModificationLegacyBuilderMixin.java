package org.kylepitcock.legacyconnector.mixin.compat;

import legacyconnector.v1_20_1.KubeJSItemBuilderCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/**
 * Restores only the inspected KubeJS 6 setItemBuilder() glow-override pattern
 * on KubeJS 7's ItemModifications record.
 */
@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.item.ItemModificationKubeEvent$ItemModifications", remap = false)
public abstract class KubeJSItemModificationLegacyBuilderMixin {
    @Unique
    public void setItemBuilder(Object builder) {
        KubeJSItemBuilderCompat.setItemBuilder(this, builder);
    }
}
