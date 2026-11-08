package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BetaEcologyB173Test {
    @BeforeAll static void bootstrap()throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    @Test void noHostedOwnershipLeavesNativeListIdentity(){
        var source=WeightedRandomList.create(new SpawnerData(EntityType.COW,12,4,4));
        assertSame(source,BetaEcologyB173.filterPopulation(source,Set.of()));
    }
    @Test void onlyHostedEntriesAreRemoved(){
        var hosted=new SpawnerData(EntityType.PIG,3,1,2);var nativeCow=new SpawnerData(EntityType.COW,12,4,4);
        var filtered=BetaEcologyB173.filterPopulation(WeightedRandomList.create(hosted,nativeCow),Set.of(EntityType.PIG));
        assertEquals(List.of(nativeCow),filtered.unwrap());assertSame(nativeCow,filtered.unwrap().getFirst());
    }
    @Test void everyUnownedEntryRetainsIdentityOrderWeightAndPackSize(){
        var nativeCow=new SpawnerData(EntityType.COW,12,4,4);var hosted=new SpawnerData(EntityType.PIG,3,1,4);
        var unownedA=new SpawnerData(EntityType.SHEEP,7,2,3);var unownedB=new SpawnerData(EntityType.SHEEP,11,1,6);
        var filtered=BetaEcologyB173.filterPopulation(WeightedRandomList.create(nativeCow,hosted,unownedA,unownedB),Set.of(EntityType.PIG));
        assertEquals(List.of(nativeCow,unownedA,unownedB),filtered.unwrap());assertSame(nativeCow,filtered.unwrap().get(0));assertSame(unownedA,filtered.unwrap().get(1));assertSame(unownedB,filtered.unwrap().get(2));
        assertEquals(7,filtered.unwrap().get(1).getWeight().asInt());assertEquals(2,filtered.unwrap().get(1).minCount);assertEquals(6,filtered.unwrap().get(2).maxCount);
    }
    @Test void fullyHostedListBecomesEmptyWithoutChangingSource(){
        var source=WeightedRandomList.create(new SpawnerData(EntityType.PIG,3,1,4));
        assertTrue(BetaEcologyB173.filterPopulation(source,Set.of(EntityType.PIG)).isEmpty());assertFalse(source.isEmpty());
    }
}
