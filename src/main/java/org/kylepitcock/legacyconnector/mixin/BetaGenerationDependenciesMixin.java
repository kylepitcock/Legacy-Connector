package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import org.kylepitcock.legacyconnector.vb1_7_3.WorldGenB173;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy population reads need real terrain throughout the native generation region. */
@Mixin(ChunkStep.Builder.class)
public abstract class BetaGenerationDependenciesMixin {
    @Shadow @Final private ChunkStatus status;
    @Shadow private int blockStateWriteRadius;
    @Shadow public abstract ChunkStep.Builder addRequirement(ChunkStatus status,int radius);

    @Inject(method="build",at=@At("HEAD"))
    private void legacyconnector$terrainPrerequisites(CallbackInfoReturnable<ChunkStep> callback){
        // The loading pyramid has write radius -1: it must keep its original dependencies.
        if(status!=ChunkStatus.FEATURES||blockStateWriteRadius!=1)return;
        boolean generators=WorldGenB173.hasGenerators();
        if(generators)addRequirement(ChunkStatus.CARVERS,8);
        WorldGenB173.recordGenerationDependencies(generators?8:1);
    }
}
