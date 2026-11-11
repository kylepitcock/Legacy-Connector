package org.kylepitcock.legacyconnector.mixin.compat;

import legacyconnector.v1_20_1.KubeJSItemBuilderCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/** Restores the KubeJS 6 ItemBuilder.glow(boolean) method using the 1.21.1 glint component. */
@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.item.ItemBuilder", remap = false)
public abstract class KubeJSItemBuilderLegacyGlowMixin {
    @Unique
    public Object glow(boolean enabled) {
        return KubeJSItemBuilderCompat.glow(this, enabled);
    }
}
