package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.ApiCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A tree grower of a legacy mod answers its feature key when the tree grows, not when the sapling block is registered (its feature holders are not bound
 * then; a mod's configured feature class reads them in its static initializer).
 */
@Mixin(TreeGrower.class)
public abstract class TreeGrowerLegacyMixin {
    @Inject(method = "getConfiguredFeature", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$legacyFeature(RandomSource random, boolean flowers, CallbackInfoReturnable<ResourceKey<ConfiguredFeature<?, ?>>> cir) {
        if (ApiCompat.legacyGrowerFeature((TreeGrower) (Object) this, random, flowers, cir)) return;
    }
}
