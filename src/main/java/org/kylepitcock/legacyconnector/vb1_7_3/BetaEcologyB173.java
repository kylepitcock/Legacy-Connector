package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.Set;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import net.minecraft.core.Holder;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

/** Keep hosted Beta fauna out of the additional native terrain-population lane. */
public final class BetaEcologyB173 {
    private BetaEcologyB173(){}
    public static WeightedRandomList<MobSpawnSettings.SpawnerData> population(Holder<Biome> biome,WeightedRandomList<MobSpawnSettings.SpawnerData> source){
        return filterPopulation(source,SpawnSchedulerB173.ownedTypes(SpawnGroup.CREATURE,biome,false));
    }
    /** Ownership is limited to hosted originals; native/foreign entries retain identity, order and weights. */
    static WeightedRandomList<MobSpawnSettings.SpawnerData> filterPopulation(WeightedRandomList<MobSpawnSettings.SpawnerData> source,Set<EntityType<?>> hosted){
        var kept=source.unwrap().stream().filter(entry->!hosted.contains(entry.type)).toList();
        return kept.size()==source.unwrap().size()?source:WeightedRandomList.create(kept);
    }
}
