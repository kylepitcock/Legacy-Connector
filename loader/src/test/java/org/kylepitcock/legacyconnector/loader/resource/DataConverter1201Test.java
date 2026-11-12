package org.kylepitcock.legacyconnector.loader.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tiny hand-written samples of the Forge 1.20.1 formats (shaped like real corpus files), converted to what NeoForge 1.21.1 reads. */
class DataConverter1201Test {
    private static JsonElement one(String path, String json, String expectedPath) {
        List<DataConverter1201.Out> outs = DataConverter1201.convert(path, json.getBytes(StandardCharsets.UTF_8), new DataConverter1201.Stats());
        assertEquals(1, outs.size());
        assertEquals(expectedPath, outs.get(0).path());
        return JsonParser.parseString(new String(outs.get(0).data(), StandardCharsets.UTF_8));
    }

    private static void same(String expected, JsonElement actual) {
        assertEquals(JsonParser.parseString(expected), actual);
    }

    @Test
    void pathsBecomeSingular() {
        assertEquals("data/m/recipe/a.json", DataConverter1201.mapPath("data/m/recipes/a.json"));
        assertEquals("data/m/loot_table/blocks/a.json", DataConverter1201.mapPath("data/m/loot_tables/blocks/a.json"));
        assertEquals("data/m/advancement/recipes/x.json", DataConverter1201.mapPath("data/m/advancements/recipes/x.json"));
        assertEquals("data/m/structure/house.nbt", DataConverter1201.mapPath("data/m/structures/house.nbt"));
        assertEquals("data/m/tags/block/ores.json", DataConverter1201.mapPath("data/m/tags/blocks/ores.json"));
        assertEquals("data/m/tags/entity_type/x.json", DataConverter1201.mapPath("data/m/tags/entity_types/x.json"));
        assertEquals("data/m/tags/worldgen/biome/x.json", DataConverter1201.mapPath("data/m/tags/worldgen/biome/x.json"));
        assertEquals("data/m/worldgen/biome/x.json", DataConverter1201.mapPath("data/m/worldgen/biome/x.json"));
        assertEquals("assets/m/models/block/x.json", DataConverter1201.mapPath("assets/m/models/block/x.json"));
    }

    @Test
    void forgeFoldersMoveToNeoForge() {
        assertEquals("data/m/neoforge/biome_modifier/x.json", DataConverter1201.mapPath("data/m/forge/biome_modifier/x.json"));
        assertEquals("data/neoforge/loot_modifiers/global_loot_modifiers.json", DataConverter1201.mapPath("data/forge/loot_modifiers/global_loot_modifiers.json"));
        assertEquals("data/c/tags/item/ingots/iron.json", DataConverter1201.mapPath("data/forge/tags/items/ingots/iron.json"));
        assertEquals("data/c/tags/block/glass_blocks/colorless.json", DataConverter1201.mapPath("data/forge/tags/blocks/glass/colorless.json"));
        assertEquals("data/c/tags/worldgen/biome/is_hot/overworld.json", DataConverter1201.mapPath("data/forge/tags/worldgen/biome/is_hot/overworld.json"));
    }

    @Test
    void shapedRecipe() {
        JsonElement r = one("data/m/recipes/a.json", """
                {"type":"minecraft:crafting_shaped","category":"misc","pattern":["#"],"key":{"#":{"tag":"forge:ingots/iron"}},
                 "result":{"item":"m:thing","count":2},"conditions":[{"type":"forge:mod_loaded","modid":"x"}]}""", "data/m/recipe/a.json");
        same("""
                {"type":"minecraft:crafting_shaped","category":"misc","pattern":["#"],"key":{"#":{"tag":"c:ingots/iron"}},
                 "result":{"id":"m:thing","count":2},"neoforge:conditions":[{"type":"neoforge:mod_loaded","modid":"x"}]}""", r);
    }

    @Test
    void smeltingAndStonecuttingResults() {
        same("{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"a:b\"},\"result\":{\"id\":\"c:d\"}}",
                one("data/m/recipes/s.json", "{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\"a:b\"},\"result\":\"c:d\"}", "data/m/recipe/s.json"));
        same("{\"type\":\"minecraft:stonecutting\",\"ingredient\":{\"item\":\"a:b\"},\"result\":{\"id\":\"c:d\",\"count\":2}}",
                one("data/m/recipes/s.json", "{\"type\":\"minecraft:stonecutting\",\"ingredient\":{\"item\":\"a:b\"},\"result\":\"c:d\",\"count\":2}", "data/m/recipe/s.json"));
    }

    @Test
    void nbtInResultsBecomesComponentsWhereKnown() {
        JsonElement r = one("data/m/recipes/p.json", """
                {"type":"minecraft:crafting_shapeless","ingredients":[{"item":"minecraft:glass_bottle"}],
                 "result":{"item":"minecraft:potion","nbt":"{Potion:\\"minecraft:water\\"}"}}""", "data/m/recipe/p.json");
        same("""
                {"type":"minecraft:crafting_shapeless","ingredients":[{"item":"minecraft:glass_bottle"}],
                 "result":{"id":"minecraft:potion","components":{"minecraft:potion_contents":{"potion":"minecraft:water"}}}}""", r);
    }

    @Test
    void modRecipeTypesKeepTheirFormat() {
        same("{\"type\":\"create:milling\",\"ingredients\":[{\"item\":\"a:b\"}],\"results\":[{\"item\":\"c:d\"}],\"neoforge:conditions\":[{\"type\":\"neoforge:mod_loaded\",\"modid\":\"x\"}]}",
                one("data/m/recipes/s.json", "{\"type\":\"create:milling\",\"ingredients\":[{\"item\":\"a:b\"}],\"results\":[{\"item\":\"c:d\"}],\"conditions\":[{\"type\":\"forge:mod_loaded\",\"modid\":\"x\"}]}", "data/m/recipe/s.json"));
    }

    @Test
    void conditionalRecipeBecomesOneRecipePerBranch() {
        String json = """
                {"type":"forge:conditional","recipes":[
                 {"conditions":[{"type":"forge:mod_loaded","modid":"a"}],"recipe":{"type":"minecraft:smelting","ingredient":{"item":"x:y"},"result":"x:z"}},
                 {"conditions":[{"type":"forge:not","value":{"type":"forge:mod_loaded","modid":"a"}}],"recipe":{"type":"minecraft:smelting","ingredient":{"item":"x:y"},"result":"x:w"}}]}""";
        List<DataConverter1201.Out> outs = DataConverter1201.convert("data/m/recipes/c.json", json.getBytes(StandardCharsets.UTF_8), new DataConverter1201.Stats());
        assertEquals(2, outs.size());
        assertEquals("data/m/recipe/c.json", outs.get(0).path());
        assertEquals("data/m/recipe/c_alt1.json", outs.get(1).path());
        String second = new String(outs.get(1).data(), StandardCharsets.UTF_8);
        assertTrue(second.contains("\"neoforge:not\"") && second.contains("\"id\":\"x:w\"") && second.contains("neoforge:conditions"), second);
    }

    @Test
    void lootTableFunctionsAndConditions() {
        JsonElement r = one("data/m/loot_tables/blocks/b.json", """
                {"type":"minecraft:block","pools":[{"rolls":1,"entries":[
                  {"type":"minecraft:item","name":"m:b","conditions":[{"condition":"minecraft:match_tool","predicate":{"enchantments":[{"enchantment":"minecraft:silk_touch","levels":{"min":1}}]}}],
                   "functions":[{"function":"minecraft:looting_enchant","count":{"min":0,"max":1}},{"function":"minecraft:set_nbt","tag":"{Potion:\\"minecraft:water\\"}"},
                                {"function":"minecraft:copy_nbt","source":"block_entity","ops":[]},{"function":"minecraft:enchant_with_levels","levels":30,"treasure":false}]},
                  {"type":"minecraft:loot_table","name":"m:other","conditions":[{"condition":"minecraft:random_chance_with_looting","chance":0.1,"looting_multiplier":0.05},
                                                                           {"condition":"forge:can_tool_perform_action","action":"shears_dig"}]}]}]}""", "data/m/loot_table/blocks/b.json");
        same("""
                {"type":"minecraft:block","pools":[{"rolls":1,"entries":[
                  {"type":"minecraft:item","name":"m:b","conditions":[{"condition":"minecraft:match_tool","predicate":{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:silk_touch","levels":{"min":1}}]}}}],
                   "functions":[{"function":"minecraft:enchanted_count_increase","count":{"min":0,"max":1},"enchantment":"minecraft:looting"},{"function":"minecraft:set_potion","id":"minecraft:water"},
                                {"function":"minecraft:copy_custom_data","source":"block_entity","ops":[]},{"function":"minecraft:enchant_with_levels","levels":30,"options":"#minecraft:non_treasure"}]},
                  {"type":"minecraft:loot_table","value":"m:other","conditions":[{"condition":"minecraft:random_chance_with_enchanted_bonus","unenchanted_chance":0.1,
                      "enchanted_chance":{"type":"minecraft:linear","base":0.15000000000000002,"per_level_above_first":0.05},"enchantment":"minecraft:looting"},
                                                                           {"condition":"neoforge:can_item_perform_ability","ability":"shears_dig"}]}]}]}""", r);
    }

    @Test
    void setAttributesGetsAnIdAndNewOperations() {
        JsonElement r = one("data/m/loot_tables/a.json", """
                {"pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"m:s","functions":[{"function":"minecraft:set_attributes","modifiers":[
                 {"name":"Power Up","attribute":"generic.attack_damage","operation":"multiply_base","amount":0.5,"slot":"mainhand","uuid":"1-2-3-4-5"}]}]}]}]}""", "data/m/loot_table/a.json");
        String s = r.toString();
        assertTrue(s.contains("\"id\":\"legacy:power_up\"") && s.contains("add_multiplied_base") && !s.contains("uuid"), s);
    }

    @Test
    void advancementIconAndPredicates() {
        JsonElement r = one("data/m/advancements/a.json", """
                {"display":{"icon":{"item":"m:x","nbt":"{Foo:1}"},"title":"t"},
                 "criteria":{"c":{"trigger":"minecraft:inventory_changed","conditions":{"items":[{"tag":"forge:ingots/iron"},{"items":["m:x"],"nbt":"{A:1}"}]}}},
                 "conditions":[{"type":"forge:mod_loaded","modid":"x"}]}""", "data/m/advancement/a.json");
        same("""
                {"display":{"icon":{"id":"m:x"},"title":"t"},
                 "criteria":{"c":{"trigger":"minecraft:inventory_changed","conditions":{"items":[{"items":"#c:ingots/iron"},{"items":["m:x"],"predicates":{"minecraft:custom_data":"{A:1}"}}]}}},
                 "neoforge:conditions":[{"type":"neoforge:mod_loaded","modid":"x"}]}""", r);
    }

    @Test
    void tagsAndRenamedIds() {
        JsonElement r = one("data/forge/tags/blocks/glass.json", "{\"replace\":false,\"values\":[\"minecraft:glass\",\"#forge:glass/colorless\",\"minecraft:grass\"]}", "data/c/tags/block/glass_blocks.json");
        same("{\"replace\":false,\"values\":[\"minecraft:glass\",\"#c:glass_blocks/colorless\",\"minecraft:short_grass\"]}", r);
    }

    @Test
    void biomeModifier() {
        JsonElement r = one("data/m/forge/biome_modifier/ore.json", "{\"type\":\"forge:add_features\",\"biomes\":\"#forge:is_overworld\",\"features\":\"m:ore\",\"step\":\"underground_ores\"}",
                "data/m/neoforge/biome_modifier/ore.json");
        same("{\"type\":\"neoforge:add_features\",\"biomes\":\"#c:is_overworld\",\"features\":\"m:ore\",\"step\":\"underground_ores\"}", r);
    }

    @Test
    void modelLoadersAndData() {
        JsonElement r = one("assets/m/models/item/x.json", "{\"loader\":\"forge:bucket\",\"parent\":\"forge:item/default\",\"fluid\":\"minecraft:water\"}", "assets/m/models/item/x.json");
        same("{\"loader\":\"neoforge:fluid_container\",\"parent\":\"neoforge:item/default\",\"fluid\":\"minecraft:water\"}", r);
        same("{\"textures\":{},\"neoforge_data\":{\"color\":1}}", one("assets/m/models/item/y.json", "{\"textures\":{},\"forge_data\":{\"color\":1}}", "assets/m/models/item/y.json"));
    }

    @Test
    void dimensionTypeGetsTheSpawnLightFields() {
        String s = one("data/m/dimension_type/d.json", "{\"ultrawarm\":false}", "data/m/dimension_type/d.json").toString();
        assertTrue(s.contains("monster_spawn_light_level") && s.contains("monster_spawn_block_light_limit"), s);
    }

    @Test
    void lenientJsonIsAccepted() {
        JsonElement r = one("data/m/recipes/l.json", "{\"type\":\"minecraft:smelting\", // comment\n \"ingredient\":{\"item\":\"a:b\"},\"result\":\"c:d\"}", "data/m/recipe/l.json");
        assertEquals("c:d", r.getAsJsonObject().getAsJsonObject("result").get("id").getAsString());
    }

    @Test
    void unparsableFilesAreKept() {
        byte[] junk = "{not json".getBytes(StandardCharsets.UTF_8);
        List<DataConverter1201.Out> outs = DataConverter1201.convert("data/m/recipes/j.json", junk, new DataConverter1201.Stats());
        assertEquals("data/m/recipe/j.json", outs.get(0).path());
    }

    @Test
    void itemModifiersMigrateLegacyFunctionIdsAndSetLoreMode() {
        JsonElement r = one("data/alienevo/item_modifiers/example.json", """
                [
                  {"function":"minecraft:set_nbt","tag":"{storedBasePrimaryScore:-1}"},
                  {"function":"minecraft:set_lore","lore":[{"text":"a"}]},
                  {"function":"minecraft:set_lore","replace":true,"lore":[{"text":"b"}]},
                  {"function":"minecraft:set_lore","replace":false,"lore":[{"text":"c"}]}
                ]
                """, "data/alienevo/item_modifier/example.json");
        String q = String.valueOf((char)34);
        assertTrue(r.toString().contains("minecraft:set_custom_data"), r.toString());
        assertTrue(r.toString().contains(q + "mode" + q + ":" + q + "append" + q), r.toString());
        assertTrue(r.toString().contains(q + "mode" + q + ":" + q + "replace_all" + q), r.toString());
        assertTrue(!r.toString().contains(q + "replace" + q + ":"), r.toString());
    }

    @Test
    void singleObjectItemModifierIsWrappedAsOneFunctionAndMigrated() {
        JsonElement r = one("data/alienevo/item_modifiers/prototype_omnitrix.json", """
                {"function":"minecraft:set_nbt","tag":"{storedBasePrimaryScore:-1}"}
                """, "data/alienevo/item_modifier/prototype_omnitrix.json");
        String quote = String.valueOf((char)34);
        assertTrue(r.isJsonArray(), r.toString());
        assertEquals(1, r.getAsJsonArray().size());
        assertTrue(r.toString().contains("minecraft:set_custom_data"), r.toString());
        assertTrue(r.toString().contains(quote + "function" + quote), r.toString());
    }

    @Test
    void palladiumAttributeIdsAreMigratedInPowerJson() {
        JsonElement r = one("data/alienevo/palladium/powers/example.json", """
                {"attribute":"forge:step_height_addition","other":{"attribute":"forge:entity_gravity"},
                 "swim":{"attribute":"forge:swim_speed"},"legacy":"minecraft:grass"}
                """, "data/alienevo/palladium/powers/example.json");
        same("""
                {"attribute":"minecraft:generic.step_height","other":{"attribute":"minecraft:generic.gravity"},
                 "swim":{"attribute":"neoforge:swim_speed"},"legacy":"minecraft:short_grass"}
                """, r);
    }

    @Test
    void unwrapsSingleComponentArraysOnlyForPalladiumNameChangeAbilities() {
        JsonElement migrated = one("data/alienevo_aliens/palladium/powers/example.json", """
                {"abilities":{
                  "name_change":{"type":"palladium:name_change","name":[{"nbt":"KubeJSPersistentData.alienevo_34","entity":"@s"}]},
                  "ordinary":{"type":"palladium:command","name":[{"text":"leave array alone"}]},
                  "multi":{"type":"palladium:name_change","name":[{"text":"A"},{"text":"B"}]}
                }}
                """, "data/alienevo_aliens/palladium/powers/example.json");
        JsonObject abilities = migrated.getAsJsonObject().getAsJsonObject("abilities");
        assertTrue(abilities.getAsJsonObject("name_change").get("name").isJsonObject(), migrated.toString());
        assertTrue(abilities.getAsJsonObject("ordinary").get("name").isJsonArray(), migrated.toString());
        assertEquals(2, abilities.getAsJsonObject("multi").getAsJsonObject().get("name").getAsJsonArray().size());
    }

    @Test
    void translatesOldParticleCommandsEmbeddedInPowerJson() {
        JsonElement r = one("data/alienevo/palladium/powers/example.json", """
                {"commands":["particle block stone ~ ~ ~ 1 0.5 1 0 30 normal",
                             "execute at @s run particle minecraft:dust 0.7 1 18000000 2 ~ ~ ~ 1 1 1 0 10"]}
                """, "data/alienevo/palladium/powers/example.json");
        String s = r.toString();
        String quote = String.valueOf((char)34);
        assertTrue(s.contains("minecraft:block{block_state:" + quote + "minecraft:stone" + quote + "}"), s);
        assertTrue(s.contains("minecraft:dust{color:[0.7,1,18000000],scale:2}"), s);
    }

    @Test
    void translatesLegacyEntityStatusEffectAndItemStackNbtInsidePowerCommands() {
        JsonElement r = one("data/alienevo_aliens/palladium/powers/example.json", """
                {"commands":[
                  "summon wolf ~ ~1 ~2 {ActiveEffects:[{Id:14,Amplifier:1,Duration:20000000,ShowParticles:0b}]}",
                  "summon piglin ~ ~1 ~2 {ArmorItems:[{},{},{},{id:oak_button,Count:1}]}"
                ]}
                """, "data/alienevo_aliens/palladium/powers/example.json");
        String value = r.toString();
        String quote = String.valueOf((char)34);
        assertTrue(value.contains("active_effects:[{id:" + quote + "minecraft:invisibility" + quote
                + ",amplifier:1,duration:20000000,show_particles:0b}]"), value);
        assertTrue(value.contains("ArmorItems:[{},{},{},{id:" + quote + "minecraft:oak_button" + quote
                + ",count:1}]"), value);
    }

    @Test
    void worldgenForgeConditionsMoveToNeoForgeAndMergeWithoutLoss() {
        JsonElement migrated = one("data/palladium/worldgen/structure/example.json", """
                {
                  "type":"minecraft:jigsaw",
                  "forge:conditions":[{"type":"palladium:feature_flag_enabled","feature_flag":"example/legacy"}],
                  "neoforge:conditions":[{"type":"neoforge:mod_loaded","modid":"example"}]
                }
                """, "data/palladium/worldgen/structure/example.json");

        JsonArray conditions = migrated.getAsJsonObject().getAsJsonArray("neoforge:conditions");
        assertEquals(2, conditions.size());
        assertEquals("neoforge:mod_loaded", conditions.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("palladium:feature_flag_enabled", conditions.get(1).getAsJsonObject().get("type").getAsString());
        assertTrue(!migrated.toString().contains("forge:conditions"), migrated.toString());
    }

    @Test
    void worldgenForgeConditionsMoveWhenNoModernKeyExists() {
        JsonElement migrated = one("data/palladium/worldgen/structure/example.json", """
                {
                  "type":"minecraft:jigsaw",
                  "forge:conditions":[{"type":"palladium:feature_flag_enabled","feature_flag":"example"}]
                }
                """, "data/palladium/worldgen/structure/example.json");

        assertTrue(migrated.getAsJsonObject().has("neoforge:conditions"), migrated.toString());
        assertTrue(!migrated.getAsJsonObject().has("forge:conditions"), migrated.toString());
    }

    @Test
    void templatePoolForgeConditionsMoveToNeoForge() {
        JsonElement migrated = one("data/palladium/worldgen/template_pool/example.json", """
                {
                  "name":"palladium:example",
                  "fallback":"minecraft:empty",
                  "elements":[],
                  "forge:conditions":[{"type":"palladium:feature_flag_enabled","feature_flag":"example"}]
                }
                """, "data/palladium/worldgen/template_pool/example.json");

        assertTrue(migrated.getAsJsonObject().has("neoforge:conditions"), migrated.toString());
        assertTrue(!migrated.getAsJsonObject().has("forge:conditions"), migrated.toString());
    }

    @Test
    void packMcmeta() {
        Map<String, byte[]> in = new LinkedHashMap<>();
        in.put("pack.mcmeta", "{\"pack\":{\"description\":\"d\",\"pack_format\":15,\"forge:data_pack_format\":12}}".getBytes(StandardCharsets.UTF_8));
        in.put("data/m/recipes/a.json", "{}".getBytes(StandardCharsets.UTF_8));
        Map<String, byte[]> out = DataConverter1201.convertAll(in, new DataConverter1201.Stats());
        String s = new String(out.get("pack.mcmeta"), StandardCharsets.UTF_8);
        assertTrue(s.contains("\"pack_format\":48") && s.contains("supported_formats") && !s.contains("forge:"), s);
        assertTrue(out.containsKey("data/m/recipe/a.json"));
    }
}
