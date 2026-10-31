package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.kylepitcock.legacyconnector.core.worldgen.BiomeOverride;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Lets legacy biome layers replace overworld land biomes (see {@link BiomeOverride}). The whole method is wrapped: TerraBlender
 * answers from a HEAD injection that returns early, and an injection at RETURN never ran in packs with it (ATM10: no Thaumcraft
 * biomes at all).
 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {
    @WrapMethod(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;")
    private Holder<Biome> legacyconnector$legacyBiomes(int x, int y, int z, Climate.Sampler sampler, Operation<Holder<Biome>> original) {
        Holder<Biome> modern = original.call(x, y, z, sampler);
        Holder<Biome> replaced = BiomeOverride.apply((BiomeSource) (Object) this, modern, x, y, z);
        return replaced != null ? replaced : modern;
    }
}
