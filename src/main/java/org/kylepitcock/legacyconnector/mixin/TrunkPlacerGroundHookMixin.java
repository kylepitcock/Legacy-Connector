package org.kylepitcock.legacyconnector.mixin;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.CoremodSites1122;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Coremod hook site (generic, {@code CoremodSites1122.TREE_GROUND}): 1.12.2 coremods put a hook in {@code WorldGenAbstractTree.setDirtAt} that keeps the
 * ground block under a tree. A no-op without such a hook.
 */
@Mixin(TrunkPlacer.class)
public abstract class TrunkPlacerGroundHookMixin {
    @Inject(method = "setDirtAt", at = @At("HEAD"), cancellable = true, require = 0)
    private static void legacyconnector$coremodTreeGround(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random, BlockPos pos,
            TreeConfiguration config, CallbackInfo ci) {
        if (!CoremodSites1122.TREE_GROUND.listed() || !(level instanceof LevelReader reader)) return;
        if (CoremodSites1122.treeKeepsGround(reader.getBlockState(pos))) ci.cancel();
    }
}
