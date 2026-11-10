package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SpawnSchedulerB173Test {
    @BeforeAll static void bootstrap()throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    private static MobSpawnSettings.SpawnerData entry(EntityType<?> type,int weight,int min,int max){
        return new MobSpawnSettings.SpawnerData(type,weight,min,max);
    }
    @Test void outsideExplicitLanePreservesTheExactNativeList(){
        var original=WeightedRandomList.create(List.of(entry(EntityType.COW,7,2,5)));
        assertSame(original,SpawnSchedulerB173.filter(null,MobCategory.CREATURE,null,null,original));
    }
    @Test void ordinaryLaneRemovesManagedEntriesWithoutChangingOrderIdentityOrWeights(){
        var cow=entry(EntityType.COW,7,2,5);
        var squid=entry(EntityType.SQUID,1,1,4);
        var sheep=entry(EntityType.SHEEP,13,3,6);
        var original=WeightedRandomList.create(List.of(cow,squid,sheep));
        var filtered=SpawnSchedulerB173.filter(original,Set.of(EntityType.SQUID),Set.of(),false,false);
        assertEquals(List.of(cow,sheep),filtered.unwrap());
        assertSame(cow,filtered.unwrap().get(0));assertSame(sheep,filtered.unwrap().get(1));
        assertEquals(7,cow.getWeight().asInt());assertEquals(13,sheep.getWeight().asInt());
        assertEquals(2,cow.minCount);assertEquals(5,cow.maxCount);
        assertEquals(List.of(cow,squid,sheep),original.unwrap());
    }
    @Test void betaLaneRetainsEverySelectedEntryIncludingDistinctWeightsForOneType(){
        var first=entry(EntityType.SQUID,1,1,4);
        var second=entry(EntityType.SQUID,9,2,3);
        var original=WeightedRandomList.create(List.of(first,entry(EntityType.COW,7,2,5),second));
        var filtered=SpawnSchedulerB173.filter(original,Set.of(EntityType.SQUID),Set.of(EntityType.SQUID),true,true);
        assertEquals(List.of(first,second),filtered.unwrap());
        assertSame(first,filtered.unwrap().get(0));assertSame(second,filtered.unwrap().get(1));
        assertEquals(1,first.getWeight().asInt());assertEquals(9,second.getWeight().asInt());
        assertEquals(2,second.minCount);assertEquals(3,second.maxCount);
        assertTrue(SpawnSchedulerB173.filter(original,Set.of(EntityType.SQUID),Set.of(EntityType.SQUID),true,false).isEmpty());
    }
    @Test void unchangedListsKeepIdentityAndEmptyListsStaySafe(){
        var original=WeightedRandomList.create(List.of(entry(EntityType.COW,7,2,5)));
        assertSame(original,SpawnSchedulerB173.filter(original,Set.of(EntityType.SQUID),Set.of(),false,false));
        assertSame(original,SpawnSchedulerB173.filter(original,Set.of(EntityType.COW),Set.of(EntityType.COW),true,true));
        WeightedRandomList<MobSpawnSettings.SpawnerData> empty=WeightedRandomList.create(List.of());
        assertSame(empty,SpawnSchedulerB173.filter(empty,Set.of(),Set.of(),false,false));
        assertTrue(SpawnSchedulerB173.filter(original,Set.of(EntityType.COW),Set.of(),false,false).isEmpty());
    }
    private static net.minecraft.world.level.biome.Biome biome(){
        return new net.minecraft.world.level.biome.Biome.BiomeBuilder().hasPrecipitation(false).temperature(1F).downfall(0F)
                .specialEffects(new net.minecraft.world.level.biome.BiomeSpecialEffects.Builder()
                        .fogColor(0).waterColor(0).waterFogColor(0).skyColor(0).build())
                .generationSettings(net.minecraft.world.level.biome.BiomeGenerationSettings.EMPTY)
                .mobSpawnSettings(new MobSpawnSettings.Builder().build()).build();
    }
    @Test void ownershipRespectsCategoryBiomeTagsAndRemovalWithoutClaimingUntouchedVanillaEntries(){
        var registry=new net.minecraft.core.MappedRegistry<net.minecraft.world.level.biome.Biome>(
                net.minecraft.core.registries.Registries.BIOME,com.mojang.serialization.Lifecycle.stable());
        var desert=registry.register(net.minecraft.world.level.biome.Biomes.DESERT,biome(),net.minecraft.core.RegistrationInfo.BUILT_IN);
        var forest=registry.register(net.minecraft.world.level.biome.Biomes.FOREST,biome(),net.minecraft.core.RegistrationInfo.BUILT_IN);
        var nether=registry.register(net.minecraft.world.level.biome.Biomes.NETHER_WASTES,biome(),net.minecraft.core.RegistrationInfo.BUILT_IN);
        registry.freeze();
        registry.bindTags(Map.of(net.minecraft.tags.BiomeTags.IS_OVERWORLD,List.of(desert,forest)));
        var monster=legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup.MONSTER;
        var creature=legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup.CREATURE;
        var policy=new SpawnSchedulerB173.Policy(EntityType.COW,monster,7,false,List.of("minecraft:desert"));
        assertEquals(Set.of(EntityType.COW),SpawnSchedulerB173.owned(List.of(policy),monster,desert,true));
        assertTrue(SpawnSchedulerB173.owned(List.of(policy),creature,desert,false).isEmpty());
        assertTrue(SpawnSchedulerB173.owned(List.of(policy),monster,forest,false).isEmpty());
        var nativeCows=WeightedRandomList.create(List.of(entry(EntityType.COW,12,4,4)));
        assertSame(nativeCows,SpawnSchedulerB173.filter(nativeCows,
                SpawnSchedulerB173.owned(List.of(policy),creature,desert,false),Set.of(),false,false));
        var overworld=new SpawnSchedulerB173.Policy(EntityType.COW,creature,3,true,List.of());
        assertEquals(Set.of(EntityType.COW),SpawnSchedulerB173.owned(List.of(overworld),creature,forest,true));
        assertTrue(SpawnSchedulerB173.owned(List.of(overworld),creature,nether,false).isEmpty());
        var removed=new SpawnSchedulerB173.Policy(EntityType.COW,creature,-1,false,List.of("minecraft:desert"));
        assertEquals(Set.of(EntityType.COW),SpawnSchedulerB173.owned(List.of(removed),creature,desert,false));
        assertTrue(SpawnSchedulerB173.owned(List.of(removed),creature,desert,true).isEmpty());
    }
    private static class RawBiome extends legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome {RawBiome(){name="Shared custom source";}}
    static class RawCreature {}
    @Test void rawCustomBiomeWithoutApiCallsUsesBetaLaneAcrossScopedKeysButLeavesNativeBiomesAlone(){
        var raw=new RawBiome();raw.spawnablePassive.add(new legacyconnector.vb1_7_3.net.minecraft.world.biome.EntitySpawnGroup(RawCreature.class,8));
        var rules=new SpawnRulesB173.Registry();rules.capture(List.of(raw));
        var registry=new net.minecraft.core.MappedRegistry<net.minecraft.world.level.biome.Biome>(net.minecraft.core.registries.Registries.BIOME,com.mojang.serialization.Lifecycle.stable());
        var firstKey=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.parse("fixture:dimension_2/shared"));
        var secondKey=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.parse("fixture:dimension_3/shared"));
        var first=registry.register(firstKey,biome(),net.minecraft.core.RegistrationInfo.BUILT_IN);
        var second=registry.register(secondKey,biome(),net.minecraft.core.RegistrationInfo.BUILT_IN);
        var ordinary=registry.register(net.minecraft.world.level.biome.Biomes.FOREST,biome(),net.minecraft.core.RegistrationInfo.BUILT_IN);registry.freeze();
        java.util.function.Function<legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome,List<String>> keys=b->List.of(firstKey.location().toString(),secondKey.location().toString());
        // Registered native types stand in for admitted original hosts; no global native registry is modified.
        var catalog=SpawnSchedulerB173.catalog(1,rules.changes(),c->EntityType.COW,keys);
        var group=legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup.CREATURE;var policies=catalog.groups().get(group);
        assertEquals(Set.of(EntityType.COW),catalog.managed(),"A raw source activates the Beta cycle without a ModLoader API call");
        assertEquals(Set.of(EntityType.COW),SpawnSchedulerB173.owned(policies,group,first,true));
        assertEquals(Set.of(EntityType.COW),SpawnSchedulerB173.owned(policies,group,second,true));
        assertTrue(SpawnSchedulerB173.owned(policies,group,ordinary,false).isEmpty());
        var cow=entry(EntityType.COW,8,1,4);var sheep=entry(EntityType.SHEEP,7,2,3);var choices=WeightedRandomList.create(List.of(cow,sheep));
        assertEquals(List.of(sheep),SpawnSchedulerB173.filter(choices,SpawnSchedulerB173.owned(policies,group,first,false),Set.of(),false,false).unwrap());
        assertSame(choices,SpawnSchedulerB173.filter(choices,SpawnSchedulerB173.owned(policies,group,ordinary,false),Set.of(),false,false));
        assertEquals(List.of(cow),SpawnSchedulerB173.filter(choices,catalog.managed(),SpawnSchedulerB173.owned(policies,group,first,true),true,true).unwrap());
        assertTrue(SpawnCapsB173.allows(group,15,256));assertFalse(SpawnCapsB173.allows(group,16,256));
        for(int weight:new int[]{0,-1}){
            if(weight==0)rules.add(RawCreature.class,0,group,new legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome[]{raw});
            else rules.remove(RawCreature.class,group,new legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome[]{raw});
            var removed=SpawnSchedulerB173.catalog(2,rules.changes(),c->EntityType.COW,keys).groups().get(group);
            assertEquals(Set.of(EntityType.COW),SpawnSchedulerB173.owned(removed,group,first,false));
            assertTrue(SpawnSchedulerB173.owned(removed,group,first,true).isEmpty());
        }
    }
}
