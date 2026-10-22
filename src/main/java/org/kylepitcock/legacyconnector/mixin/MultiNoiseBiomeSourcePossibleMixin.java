package org.kylepitcock.legacyconnector.mixin;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.kylepitcock.legacyconnector.core.worldgen.BiomeOverride;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The chunk generator schedules the features of the biome source's possible biomes only; the overworld source must
 * list the legacy biomes it places through {@link BiomeOverride}, or their decoration never runs.
 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourcePossibleMixin {
    @Inject(method = "collectPossibleBiomes", at = @At("RETURN"), cancellable = true)
    private void legacyconnector$legacyBiomes(CallbackInfoReturnable<Stream<Holder<Biome>>> cir) {
        List<Holder<Biome>> extra = BiomeOverride.extraBiomes();
        if (extra.isEmpty()) return;
        List<Holder<Biome>> own = cir.getReturnValue().toList();
        if (own.stream().noneMatch(h -> h.is(Biomes.PLAINS))) {
            cir.setReturnValue(own.stream());
            return;
        }
        cir.setReturnValue(Stream.concat(own.stream(), extra.stream()));
    }
}
