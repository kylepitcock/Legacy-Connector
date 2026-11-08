package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.kylepitcock.legacyconnector.vb1_7_3.BetaEcologyB173;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Only the native generation-fauna list is filtered; biome decoration remains untouched. */
@Mixin(NaturalSpawner.class)
public abstract class BetaChunkPopulationMixin {
    @WrapOperation(method="spawnMobsForChunkGeneration",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/biome/MobSpawnSettings;getMobs(Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/util/random/WeightedRandomList;"))
    private static WeightedRandomList<MobSpawnSettings.SpawnerData> legacyconnector$betaPopulation(MobSpawnSettings settings,MobCategory category,Operation<WeightedRandomList<MobSpawnSettings.SpawnerData>> original,@Local(argsOnly=true) Holder<Biome> biome){
        var entries=original.call(settings,category);
        return category==MobCategory.CREATURE?BetaEcologyB173.population(biome,entries):entries;
    }
}
