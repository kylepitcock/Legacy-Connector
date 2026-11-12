package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Narrow access to the active 1.21.1 multi-noise source climate list. */
@Mixin(MultiNoiseBiomeSource.class)
public interface MultiNoiseBiomeSourceAccessor {
    @Invoker("parameters")
    Climate.ParameterList<Holder<Biome>> legacyconnector$parameters();
}
