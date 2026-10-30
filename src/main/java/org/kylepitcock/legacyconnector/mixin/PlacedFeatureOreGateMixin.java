package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla ore features ask 1.7.10's ore event first (see {@code VanillaOreGate}). */
@Mixin(PlacedFeature.class)
public abstract class PlacedFeatureOreGateMixin {
    @Inject(method = "placeWithBiomeCheck", at = @At("HEAD"), cancellable = true)
    private void legacyconnector$oreGate(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin,
            CallbackInfoReturnable<Boolean> cir) {
        if (!org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOreGate.allow((PlacedFeature) (Object) this, level, random, origin)) {
            cir.setReturnValue(false);
        }
    }
}
