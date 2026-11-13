package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.kylepitcock.legacyconnector.core.worldgen.BiomePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Uses the connector-owned climate list only for the exact overworld source bound by LegacyBiomeLayers.
 * All other multi-noise sources, including other dimensions and custom legacy world-type sources, retain vanilla behavior.
 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceLegacyBiomeMixin {
    @Inject(
            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void legacyconnector$useNativeLegacyBiomePlacement(int x, int y, int z, Climate.Sampler sampler,
                                                                 CallbackInfoReturnable<Holder<Biome>> cir) {
        Climate.ParameterList<Holder<Biome>> parameters =
                BiomePlacement.nativeParametersFor((MultiNoiseBiomeSource) (Object) this);
        if (parameters != null) {
            cir.setReturnValue(parameters.findValue(sampler.sample(x, y, z)));
        }
    }
}
