package org.kylepitcock.legacyconnector.mixin;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Legacy village buildings are added to a vanilla village after vanilla assembled it, unchanged
 * (LegacyVillageAdditions): only jigsaw structures starting from a vanilla village pool are touched.
 */
@Mixin(JigsawStructure.class)
public abstract class JigsawStructureLegacyVillageMixin {
    @Shadow
    @Final
    private Holder<StructureTemplatePool> startPool;

    @Inject(method = "findGenerationPoint", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$legacyVillageBuildings(Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        try {
            cir.setReturnValue(org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyVillageAdditions.apply(cir.getReturnValue(), context, startPool));
        } catch (RuntimeException | LinkageError e) {
            // the village stays vanilla
        }
    }
}
