package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import legacyconnector.vb1_7_3.net.minecraft.entity.SpawnGroup;
import legacyconnector.vb1_7_3.net.minecraft.world.biome.Biome;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class SpawnRulesB173Test {
    static class Creature {}
    static class UnknownBiome extends Biome {UnknownBiome(){super();name="Unbridged";}}
    @Test void defaultBiomesReplaceWeightsAndExplicitRemovalLeavesOtherBiomes() {
        var registry=new SpawnRulesB173.Registry();
        registry.add(Creature.class,6,SpawnGroup.CREATURE,null);
        assertEquals(11,registry.entries().getFirst().biomes().size());
        registry.add(Creature.class,8,SpawnGroup.CREATURE,new Biome[]{Biome.FOREST});
        assertEquals(11,registry.entries().stream().mapToInt(e->e.biomes().size()).sum());
        assertEquals(8,registry.entries().stream().filter(e->e.biomes().contains(Biome.FOREST)).findFirst().orElseThrow().weight());
        registry.remove(Creature.class,SpawnGroup.CREATURE,new Biome[]{Biome.FOREST});
        assertEquals(10,registry.entries().stream().mapToInt(e->e.biomes().size()).sum());
        registry.remove(Creature.class,SpawnGroup.CREATURE,null);assertTrue(registry.entries().isEmpty());
        registry.add(Creature.class,4,SpawnGroup.CREATURE,new Biome[]{});assertTrue(registry.entries().isEmpty());
    }
    @Test void generatesNativeModifiersWithOriginalWeightAndExplicitBiomeFamilies() {
        var registry=new SpawnRulesB173.Registry();registry.add(Creature.class,6,SpawnGroup.CREATURE,null);
        Map<String,Supplier<byte[]>> out=new LinkedHashMap<>();
        SpawnRulesB173.contribute(out,registry.entries(),c->ResourceLocation.parse("fixture:creature"));
        var modifier=JsonParser.parseString(new String(out.values().iterator().next().get(),StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("neoforge:add_spawns",modifier.get("type").getAsString());
        assertEquals("#minecraft:is_overworld",modifier.get("biomes").getAsString());
        var spawner=modifier.getAsJsonObject("spawners");assertEquals(6,spawner.get("weight").getAsInt());
        assertEquals(1,spawner.get("minCount").getAsInt());assertEquals(4,spawner.get("maxCount").getAsInt());
        out.clear();registry.remove(Creature.class,SpawnGroup.CREATURE,null);
        registry.add(Creature.class,8,SpawnGroup.CREATURE,new Biome[]{Biome.FOREST,Biome.SEASONAL_FOREST});
        SpawnRulesB173.contribute(out,registry.entries(),c->ResourceLocation.parse("fixture:creature"));
        String tag=out.entrySet().stream().filter(e->e.getKey().contains("tags/worldgen/biome")).map(e->new String(e.getValue().get(),StandardCharsets.UTF_8)).findFirst().orElseThrow();
        assertTrue(tag.contains("minecraft:forest"));assertTrue(tag.contains("c:"));
        assertEquals(1,JsonParser.parseString(tag).getAsJsonObject().getAsJsonArray("values").asList().stream().filter(v->v.isJsonPrimitive()&&v.getAsString().equals("minecraft:forest")).count());
    }
    @Test void skipsUnhostedZeroWeightAndUnbridgedBiomeEntries() {
        var registry=new SpawnRulesB173.Registry();Map<String,Supplier<byte[]>> out=new LinkedHashMap<>();
        registry.add(Creature.class,6,SpawnGroup.CREATURE,null);
        SpawnRulesB173.contribute(out,registry.entries(),c->null);assertTrue(out.isEmpty());
        registry.add(Creature.class,0,SpawnGroup.CREATURE,null);
        SpawnRulesB173.contribute(out,registry.entries(),c->ResourceLocation.parse("fixture:creature"));assertTrue(out.isEmpty());
        registry.remove(Creature.class,SpawnGroup.CREATURE,null);
        registry.add(Creature.class,6,SpawnGroup.CREATURE,new Biome[]{new UnknownBiome()});
        SpawnRulesB173.contribute(out,registry.entries(),c->ResourceLocation.parse("fixture:creature"));assertTrue(out.isEmpty());
    }
    @Test void originalSpawnGroupOverridesAmbiguousFamilyAndConflictsRemainDiagnosed() {
        var registry=new SpawnRulesB173.Registry();
        registry.add(Creature.class,8,SpawnGroup.MONSTER,new Biome[]{Biome.FOREST});
        assertEquals(net.minecraft.world.entity.MobCategory.MONSTER,SpawnRulesB173.categoryFor(Creature.class,net.minecraft.world.entity.MobCategory.MISC,registry.entries()));
        registry.add(Creature.class,0,SpawnGroup.CREATURE,new Biome[]{Biome.FOREST});
        assertEquals(net.minecraft.world.entity.MobCategory.MONSTER,SpawnRulesB173.categoryFor(Creature.class,net.minecraft.world.entity.MobCategory.MISC,registry.entries()));
        registry.add(Creature.class,4,SpawnGroup.CREATURE,new Biome[]{Biome.FOREST});
        assertEquals(net.minecraft.world.entity.MobCategory.MISC,SpawnRulesB173.categoryFor(Creature.class,net.minecraft.world.entity.MobCategory.MISC,registry.entries()));
    }

    @Test void removalTombstonesRemainAuthoritativeAcrossAddRemoveAddCalls(){
        var registry=new SpawnRulesB173.Registry();
        registry.remove(Creature.class,SpawnGroup.CREATURE,new Biome[]{Biome.FOREST});
        assertTrue(registry.entries().isEmpty());assertEquals(-1,registry.changes().getFirst().weight());
        registry.add(Creature.class,4,SpawnGroup.CREATURE,null);assertEquals(11,registry.entries().getFirst().biomes().size());assertEquals(1,registry.changes().size());
        registry.remove(Creature.class,SpawnGroup.CREATURE,new Biome[]{Biome.FOREST});
        assertEquals(10,registry.entries().getFirst().biomes().size());assertEquals(1,registry.changes().stream().filter(e->e.weight()<0).count());
        registry.add(Creature.class,9,SpawnGroup.CREATURE,new Biome[]{Biome.FOREST});
        assertEquals(0,registry.changes().stream().filter(e->e.weight()<0).count());
        assertEquals(9,registry.entries().stream().filter(e->e.biomes().contains(Biome.FOREST)).findFirst().orElseThrow().weight());
    }
    @Test void explicitRemovalGeneratesOneAuthoritativeModifierForOnlyRequestedCategoryAndBiomes(){
        var registry=new SpawnRulesB173.Registry();registry.remove(Creature.class,SpawnGroup.CREATURE,new Biome[]{Biome.FOREST});
        var out=new LinkedHashMap<String,Supplier<byte[]>>();SpawnRulesB173.contribute(out,registry.changes(),c->ResourceLocation.parse("fixture:creature"));
        var json=out.entrySet().stream().filter(e->e.getKey().contains("biome_modifier")).map(e->JsonParser.parseString(new String(e.getValue().get(),StandardCharsets.UTF_8)).getAsJsonObject()).findFirst().orElseThrow();
        assertEquals("legacyconnector:beta_spawn_override",json.get("type").getAsString());assertEquals("fixture:creature",json.get("entity").getAsString());
        assertEquals("creature",json.get("category").getAsString());assertEquals(0,json.get("weight").getAsInt());assertNotEquals("#minecraft:is_overworld",json.get("biomes").getAsString());
        registry.add(Creature.class,7,SpawnGroup.MONSTER,new Biome[]{Biome.FOREST});registry.remove(Creature.class,SpawnGroup.CREATURE,null);
        assertEquals(SpawnGroup.MONSTER,registry.entries().getFirst().group());assertEquals(7,registry.entries().getFirst().weight());
    }
}
