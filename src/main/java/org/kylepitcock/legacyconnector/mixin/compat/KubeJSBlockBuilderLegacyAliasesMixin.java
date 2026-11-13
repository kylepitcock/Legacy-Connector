package org.kylepitcock.legacyconnector.mixin.compat;

import legacyconnector.v1_20_1.KubeJSBlockBuilderCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

/**
 * KubeJS 7 removed the KubeJS 6 BlockBuilder.model() and textureAll() methods.
 * Restore their old script-facing contracts for translated Forge 1.20.1 scripts.
 *
 * <p>The target is optional and string-based so Legacy Connector does not gain
 * a hard runtime dependency on KubeJS.</p>
 */
@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.block.BlockBuilder", remap = false)
public abstract class KubeJSBlockBuilderLegacyAliasesMixin {
    @Unique
    public Object model(String model) {
        return KubeJSBlockBuilderCompat.model(this, model);
    }

    @Unique
    public Object textureAll(String texture) {
        return KubeJSBlockBuilderCompat.textureAll(this, texture);
    }
}
