package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SpawnOverrideModifierB173Test {
    @BeforeAll static void bootstrap()throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    private static Biome biome(){
        return new Biome.BiomeBuilder().hasPrecipitation(true).temperature(.8F).downfall(.4F)
                .specialEffects(new BiomeSpecialEffects.Builder().fogColor(0).waterColor(0).waterFogColor(0).skyColor(0).build())
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .mobSpawnSettings(new MobSpawnSettings.Builder()
                        .addSpawn(MobCategory.WATER_CREATURE,new MobSpawnSettings.SpawnerData(EntityType.SQUID,10,2,4))
                        .addSpawn(MobCategory.WATER_CREATURE,new MobSpawnSettings.SpawnerData(EntityType.SQUID,5,1,2))
                        .addSpawn(MobCategory.CREATURE,new MobSpawnSettings.SpawnerData(EntityType.SQUID,4,1,1))
                        .addSpawn(MobCategory.CREATURE,new MobSpawnSettings.SpawnerData(EntityType.COW,12,4,4)).build()).build();
    }
    private static ModifiableBiomeInfo.BiomeInfo.Builder builder(Biome biome){return ModifiableBiomeInfo.BiomeInfo.Builder.copyOf(biome.modifiableBiomeInfo().getOriginalBiomeInfo());}
    @Test void exactBetaVanillaClassesReuseSharedTableWithoutMatchingModSubclasses(){
        assertSame(EntityType.SQUID,SpawnRulesB173.vanillaTypeNamed("legacyconnector_synth.vb1_7_3.net.minecraft.entity.passive.SquidEntity"));
        assertSame(EntityType.COW,SpawnRulesB173.vanillaTypeNamed("legacyconnector.vb1_7_3.net.minecraft.entity.passive.CowEntity"));
        assertSame(EntityType.ZOMBIFIED_PIGLIN,SpawnRulesB173.vanillaTypeNamed("legacyconnector_synth.vb1_7_3.net.minecraft.entity.mob.PigZombieEntity"));
        assertSame(EntityType.GIANT,SpawnRulesB173.vanillaTypeNamed("legacyconnector_synth.vb1_7_3.net.minecraft.entity.mob.GiantEntity"));
        assertNull(SpawnRulesB173.vanillaTypeNamed("legacyconnector_b173.example.SquidEntity"));
        assertNull(SpawnRulesB173.vanillaTypeNamed("legacyconnector.vb1_7_3.net.minecraft.entity.passive.NotVanillaEntity"));
        assertNull(SpawnRulesB173.vanillaTypeNamed("legacyconnector.vb1_7_3.net.minecraft.entity.passive.SquidEntitySubclass"));
        assertNull(org.kylepitcock.legacyconnector.v1_7_10.entity.VanillaEntities.modernTypeNamed("passive.EntityNotVanilla"));
    }
    @Test void authoritativeReplacementRunsAfterNativeAddAndRemoveAndIsIdempotent(){
        var biome=biome();var holder=Holder.direct(biome);var builder=builder(biome);
        var modifier=new SpawnOverrideModifierB173(HolderSet.direct(holder),EntityType.SQUID,MobCategory.WATER_CREATURE,1);
        for(var phase:List.of(BiomeModifier.Phase.BEFORE_EVERYTHING,BiomeModifier.Phase.ADD,BiomeModifier.Phase.REMOVE)){
            modifier.modify(holder,phase,builder);assertEquals(2,builder.getMobSpawnSettings().getSpawner(MobCategory.WATER_CREATURE).size());
        }
        // Native remove_spawns runs in REMOVE and may remove vanilla entries; our replacement must survive it.
        builder.getMobSpawnSettings().getSpawner(MobCategory.WATER_CREATURE).clear();
        modifier.modify(holder,BiomeModifier.Phase.MODIFY,builder);modifier.modify(holder,BiomeModifier.Phase.MODIFY,builder);
        var entries=builder.getMobSpawnSettings().getSpawner(MobCategory.WATER_CREATURE);assertEquals(1,entries.size());
        assertSame(EntityType.SQUID,entries.getFirst().type);assertEquals(1,entries.getFirst().getWeight().asInt());assertEquals(1,entries.getFirst().minCount);assertEquals(4,entries.getFirst().maxCount);
        assertEquals(2,builder.getMobSpawnSettings().getSpawner(MobCategory.CREATURE).size());
        modifier.modify(holder,BiomeModifier.Phase.AFTER_EVERYTHING,builder);assertEquals(1,entries.size());
    }
    @Test void removalIsCategoryAndBiomeSpecificAndRemovesEveryMatchingEntry(){
        var biome=biome();var holder=Holder.direct(biome);var builder=builder(biome);
        var modifier=new SpawnOverrideModifierB173(HolderSet.direct(holder),EntityType.SQUID,MobCategory.WATER_CREATURE,0);
        modifier.modify(Holder.direct(biome()),BiomeModifier.Phase.MODIFY,builder);assertEquals(2,builder.getMobSpawnSettings().getSpawner(MobCategory.WATER_CREATURE).size());
        modifier.modify(holder,BiomeModifier.Phase.MODIFY,builder);assertTrue(builder.getMobSpawnSettings().getSpawner(MobCategory.WATER_CREATURE).isEmpty());
        var other=builder.getMobSpawnSettings().getSpawner(MobCategory.CREATURE);assertEquals(2,other.size());assertSame(EntityType.COW,other.get(1).type);
    }
    @Test void vanillaWeightRequestsEmitAuthoritativeReplacementRatherThanDuplicateAdd(){
        var registry=new SpawnRulesB173.Registry();registry.add(legacyconnector.vb1_7_3.net.minecraft.entity.passive.CowEntity.class,3,legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup.CREATURE,null);
        var out=new java.util.LinkedHashMap<String,java.util.function.Supplier<byte[]>>();
        SpawnRulesB173.contribute(out,registry.changes(),type->net.minecraft.resources.ResourceLocation.withDefaultNamespace("cow"));
        var json=com.google.gson.JsonParser.parseString(new String(out.values().iterator().next().get(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("legacyconnector:beta_spawn_override",json.get("type").getAsString());assertEquals("minecraft:cow",json.get("entity").getAsString());assertEquals(3,json.get("weight").getAsInt());
        assertFalse(json.has("spawners"));
    }}
