package org.kylepitcock.legacyconnector.loader.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
                {"display":{"icon":{"id":"m:x","components":{"minecraft:custom_data":"{Foo:1}"}},"title":"t"},
                 "criteria":{"c":{"trigger":"minecraft:inventory_changed","conditions":{"items":[{"items":"#c:ingots/iron"},{"items":["m:x"],"predicates":{"minecraft:custom_data":"{A:1}"}}]}}},
                 "neoforge:conditions":[{"type":"neoforge:mod_loaded","modid":"x"}]}""", r);
    }

    @Test
    void advancementLocationStructurePredicateIsPluralizedFor1211() {
        String source = """
                {
                  "criteria": {
                    "find_crash_site": {
                      "trigger": "minecraft:location",
                      "conditions": {
                        "player": [
                          {
                            "condition": "minecraft:entity_properties",
                            "entity": "this",
                            "predicate": {
                              "location": {
                                "structure": "alienevo:crash_site"
                              }
                            }
                          }
                        ]
                      }
                    }
                  },
                  "requirements": [["find_crash_site"]]
                }
                """;
        JsonElement converted = one(
                "data/alienevo/advancements/discover_ship.json",
                source,
                "data/alienevo/advancement/discover_ship.json");
        JsonObject location = converted.getAsJsonObject()
                .getAsJsonObject("criteria")
                .getAsJsonObject("find_crash_site")
                .getAsJsonObject("conditions")
                .getAsJsonArray("player")
                .get(0).getAsJsonObject()
                .getAsJsonObject("predicate")
                .getAsJsonObject("location");
        assertEquals("alienevo:crash_site", location.get("structures").getAsString());
        assertTrue(!location.has("structure"), converted.toString());

        List<DataConverter1201.Out> secondPass = DataConverter1201.convert(
                "data/alienevo/advancement/discover_ship.json",
                converted.toString().getBytes(StandardCharsets.UTF_8),
                new DataConverter1201.Stats());
        assertEquals(1, secondPass.size());
        assertEquals(converted, JsonParser.parseString(
                new String(secondPass.get(0).data(), StandardCharsets.UTF_8)));
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
        JsonArray commands = r.getAsJsonObject().getAsJsonArray("commands");
        String blockCommand = commands.get(0).getAsString();
        String dustCommand = commands.get(1).getAsString();
        String quote = String.valueOf((char)34);
        assertTrue(blockCommand.contains("minecraft:block{block_state:" + quote + "minecraft:stone" + quote + "}"), blockCommand);
        assertTrue(dustCommand.contains("minecraft:dust{color:[0.7,1.0,18000000.0],scale:2.0}"), dustCommand);
    }

    @Test
    void translatesLegacyEntityStatusEffectAndItemStackNbtInsidePowerCommands() {
        JsonElement r = one("data/alienevo_aliens/palladium/powers/example.json", """
                {"commands":[
                  "summon wolf ~ ~1 ~2 {ActiveEffects:[{Id:14,Amplifier:1,Duration:20000000,ShowParticles:0b}]}",
                  "summon piglin ~ ~1 ~2 {ArmorItems:[{},{},{},{id:oak_button,Count:1}]}"
                ]}
                """, "data/alienevo_aliens/palladium/powers/example.json");
        JsonArray commands = r.getAsJsonObject().getAsJsonArray("commands");
        String effectsCommand = commands.get(0).getAsString();
        String armorCommand = commands.get(1).getAsString();
        String quote = String.valueOf((char)34);
        assertTrue(effectsCommand.contains("active_effects:[{id:" + quote + "minecraft:invisibility" + quote
                + ",amplifier:1,duration:20000000,show_particles:0b}]"), effectsCommand);
        assertTrue(armorCommand.contains("ArmorItems:[{},{},{},{id:" + quote + "minecraft:oak_button" + quote
                + ",count:1}]"), armorCommand);
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
        assertTrue(!migrated.getAsJsonObject().has("forge:conditions"), migrated.toString());
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

    // ---- structured item NBT to data components (ItemComponents1201), entity targets, convention tags --------------------------------

    @Test
    void recipeResultNbtBecomesEveryComponentMinecraftOwns() {
        JsonElement r = one("data/m/recipes/x.json", """
                {"type":"minecraft:crafting_shapeless","ingredients":[{"item":"minecraft:stick"}],
                 "result":{"item":"minecraft:diamond_sword","count":1,"nbt":"{Damage:5,RepairCost:2,Unbreakable:1b,CustomModelData:7,HideFlags:3,display:{Name:'{\\"text\\":\\"Blade\\"}',Lore:['{\\"text\\":\\"a\\"}','plain'],color:255},Enchantments:[{id:\\"minecraft:sharpness\\",lvl:3s}],mymod:{level:4b}}"}}""",
                "data/m/recipe/x.json");
        JsonObject comps = r.getAsJsonObject().getAsJsonObject("result").getAsJsonObject("components");
        assertEquals(5, comps.get("minecraft:damage").getAsInt());
        assertEquals(2, comps.get("minecraft:repair_cost").getAsInt());
        assertEquals(7, comps.get("minecraft:custom_model_data").getAsInt());
        // HideFlags 3: enchantments (1) and attribute modifiers (2) hidden; there are no attribute modifiers to hide
        same("{\"levels\":{\"minecraft:sharpness\":3},\"show_in_tooltip\":false}", comps.get("minecraft:enchantments"));
        same("{\"text\":\"Blade\"}", comps.get("minecraft:custom_name"));
        same("[{\"text\":\"a\"},\"plain\"]", comps.get("minecraft:lore"));
        same("{\"rgb\":255}", comps.get("minecraft:dyed_color"));
        same("{}", comps.get("minecraft:unbreakable"));
        assertEquals("{mymod:{level:4b}}", comps.get("minecraft:custom_data").getAsString());
    }

    @Test
    void attributeModifiersBannerPatternsStoredEnchantmentsAndHiddenFlags() {
        JsonElement r = one("data/m/recipes/y.json", """
                {"type":"minecraft:crafting_shapeless","ingredients":[{"item":"minecraft:stick"}],
                 "result":{"item":"minecraft:white_banner","nbt":"{BlockEntityTag:{Patterns:[{Pattern:\\"bs\\",Color:14}]},HideFlags:32,AttributeModifiers:[{AttributeName:\\"generic.attackDamage\\",Name:\\"n\\",Amount:2.5d,Operation:0,UUID:[I;1,2,3,4],Slot:\\"mainhand\\"}]}"}}""",
                "data/m/recipe/y.json");
        JsonObject comps = r.getAsJsonObject().getAsJsonObject("result").getAsJsonObject("components");
        same("[{\"color\":\"red\",\"pattern\":\"minecraft:stripe_bottom\"}]", comps.get("minecraft:banner_patterns"));
        same("{}", comps.get("minecraft:hide_additional_tooltip"));
        JsonObject modifier = comps.getAsJsonArray("minecraft:attribute_modifiers").get(0).getAsJsonObject();
        assertEquals("minecraft:generic.attack_damage", modifier.get("type").getAsString());
        assertEquals("add_value", modifier.get("operation").getAsString());
        assertEquals("mainhand", modifier.get("slot").getAsString());
        assertEquals(2.5, modifier.get("amount").getAsDouble());
        assertEquals("legacy:00000001-0000-0002-0000-000300000004", modifier.get("id").getAsString());

        JsonElement book = one("data/m/recipes/z.json", """
                {"type":"minecraft:crafting_shapeless","ingredients":[{"item":"minecraft:stick"}],
                 "result":{"item":"minecraft:enchanted_book","nbt":"{StoredEnchantments:[{id:\\"minecraft:mending\\",lvl:1s}]}"}}""", "data/m/recipe/z.json");
        same("{\"minecraft:stored_enchantments\":{\"levels\":{\"minecraft:mending\":1}}}",
                book.getAsJsonObject().getAsJsonObject("result").get("components"));
    }

    @Test
    void nbtThatCannotBeCarriedOverStaysInTheCustomData() {
        // Trim has no exact form here, a bad Name type is refused: both stay readable in custom_data instead of vanishing
        JsonElement r = one("data/m/recipes/w.json", """
                {"type":"minecraft:crafting_shapeless","ingredients":[{"item":"minecraft:stick"}],
                 "result":{"item":"minecraft:iron_chestplate","nbt":"{Trim:{material:\\"minecraft:gold\\"},Damage:2}"}}""", "data/m/recipe/w.json");
        JsonObject comps = r.getAsJsonObject().getAsJsonObject("result").getAsJsonObject("components");
        assertEquals(2, comps.get("minecraft:damage").getAsInt());
        assertEquals("{Trim:{material:\"minecraft:gold\"}}", comps.get("minecraft:custom_data").getAsString());
    }

    @Test
    void nbtIngredientsKeepTheirComponentsAndOnlyStrictOnesTheCustomData() {
        JsonElement r = one("data/m/recipes/i.json", """
                {"type":"minecraft:crafting_shapeless","ingredients":[
                  {"type":"forge:nbt","item":"minecraft:potion","nbt":"{Potion:\\"minecraft:healing\\",extra:1}"},
                  {"type":"forge:strict_nbt","item":"m:x","nbt":"{Damage:3,extra:1}"}],
                 "result":{"item":"minecraft:stick"}}""", "data/m/recipe/i.json");
        JsonArray ing = r.getAsJsonObject().getAsJsonArray("ingredients");
        same("{\"type\":\"neoforge:components\",\"items\":\"minecraft:potion\",\"components\":{\"minecraft:potion_contents\":{\"potion\":\"minecraft:healing\"}},\"strict\":false}", ing.get(0));
        same("{\"type\":\"neoforge:components\",\"items\":\"m:x\",\"components\":{\"minecraft:damage\":3,\"minecraft:custom_data\":\"{extra:1}\"},\"strict\":true}", ing.get(1));
    }

    @Test
    void setNbtSplitsIntoComponentsAndCustomDataByTheItemInScope() {
        JsonElement r = one("data/m/loot_tables/blocks/b.json", """
                {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"minecraft:diamond_pickaxe",
                  "functions":[{"function":"minecraft:set_nbt","tag":"{Damage:7,Enchantments:[{id:\\"minecraft:efficiency\\",lvl:2s}],Owner:\\"x\\"}"}]}]}]}""",
                "data/m/loot_table/blocks/b.json");
        JsonArray fns = r.getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("functions");
        assertEquals(2, fns.size(), fns.toString());
        assertEquals("minecraft:set_components", fns.get(0).getAsJsonObject().get("function").getAsString());
        same("{\"minecraft:damage\":7,\"minecraft:enchantments\":{\"levels\":{\"minecraft:efficiency\":2}}}", fns.get(0).getAsJsonObject().get("components"));
        assertEquals("minecraft:set_custom_data", fns.get(1).getAsJsonObject().get("function").getAsString());
        assertEquals("{Owner:\"x\"}", fns.get(1).getAsJsonObject().get("tag").getAsString());
    }

    @Test
    void setNbtWithConditionsKeepsOneFunctionAndAPotionOnAnUnknownItemStaysCustomData() {
        JsonElement r = one("data/m/loot_tables/blocks/c.json", """
                {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"m:thing",
                  "functions":[{"function":"minecraft:set_nbt","tag":"{display:{Name:'\\"X\\"'},mod:1}","conditions":[{"condition":"minecraft:survives_explosion"}]},
                               {"function":"minecraft:set_nbt","tag":"{Potion:\\"minecraft:water\\",mod:2}"}]}]}]}""", "data/m/loot_table/blocks/c.json");
        JsonArray fns = r.getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("functions");
        assertEquals(2, fns.size(), fns.toString());
        JsonObject first = fns.get(0).getAsJsonObject();
        assertEquals("minecraft:set_components", first.get("function").getAsString());
        assertEquals("{mod:1}", first.getAsJsonObject("components").get("minecraft:custom_data").getAsString());
        assertEquals("minecraft:survives_explosion", first.getAsJsonArray("conditions").get(0).getAsJsonObject().get("condition").getAsString());
        // m:thing is not Minecraft's: Potion is the mod's own key there
        assertEquals("minecraft:set_custom_data", fns.get(1).getAsJsonObject().get("function").getAsString());
    }

    @Test
    void itemModifierSetNbtIsSplitInsideTheFunctionList() {
        JsonElement r = one("data/m/item_modifiers/s.json", """
                [{"function":"minecraft:set_nbt","tag":"{CustomModelData:3,k:1}"},{"function":"minecraft:set_count","count":2}]""", "data/m/item_modifier/s.json");
        JsonArray a = r.getAsJsonArray();
        assertEquals(3, a.size(), a.toString());
        assertEquals("minecraft:set_components", a.get(0).getAsJsonObject().get("function").getAsString());
        assertEquals(3, a.get(0).getAsJsonObject().getAsJsonObject("components").get("minecraft:custom_model_data").getAsInt());
        assertEquals("minecraft:set_custom_data", a.get(1).getAsJsonObject().get("function").getAsString());
        assertEquals("minecraft:set_count", a.get(2).getAsJsonObject().get("function").getAsString());
    }

    @Test
    void advancementIconKeepsItsModelNumberAndCustomData() {
        JsonElement r = one("data/m/advancements/i.json", """
                {"display":{"icon":{"item":"m:x","nbt":"{CustomModelData:2,Foo:1}"},"title":"t"},"criteria":{}}""", "data/m/advancement/i.json");
        JsonObject icon = r.getAsJsonObject().getAsJsonObject("display").getAsJsonObject("icon");
        assertEquals("m:x", icon.get("id").getAsString());
        assertEquals(2, icon.getAsJsonObject("components").get("minecraft:custom_model_data").getAsInt());
        assertEquals("{Foo:1}", icon.getAsJsonObject("components").get("minecraft:custom_data").getAsString());
    }

    @Test
    void itemPredicateNbtOfPotionEnchantmentsAndDamageBecomesSubPredicates() {
        JsonElement r = one("data/m/advancements/p.json", """
                {"criteria":{"c":{"trigger":"minecraft:inventory_changed","conditions":{"items":[
                  {"items":["minecraft:potion"],"nbt":"{Potion:\\"minecraft:healing\\"}"},
                  {"items":["minecraft:diamond_sword"],"nbt":"{Damage:0,Enchantments:[{id:\\"minecraft:sharpness\\",lvl:5s}],mod:1}"},
                  {"items":["minecraft:stick"],"nbt":"{display:{Name:'\\"X\\"'}}"}]}}}}""", "data/m/advancement/p.json");
        JsonArray items = r.getAsJsonObject().getAsJsonObject("criteria").getAsJsonObject("c").getAsJsonObject("conditions").getAsJsonArray("items");
        same("{\"items\":[\"minecraft:potion\"],\"predicates\":{\"minecraft:potion_contents\":\"minecraft:healing\"}}", items.get(0));
        same("{\"items\":[\"minecraft:diamond_sword\"],\"predicates\":{\"minecraft:enchantments\":[{\"enchantments\":\"minecraft:sharpness\",\"levels\":{\"min\":5,\"max\":5}}],\"minecraft:custom_data\":\"{mod:1}\"}}", items.get(1));
        // a name has no sub-predicate: the old behaviour (a custom data test of the NBT) stays
        assertTrue(items.get(2).getAsJsonObject().getAsJsonObject("predicates").has("minecraft:custom_data"));
    }

    @Test
    void killerTargetsAreRenamedInLootConditionsFunctionsAndPredicates() {
        JsonElement r = one("data/m/loot_tables/entities/e.json", """
                {"type":"minecraft:entity","pools":[{"rolls":1,"conditions":[{"condition":"minecraft:entity_properties","entity":"killer","predicate":{}},
                  {"condition":"minecraft:entity_properties","entity":"direct_killer","predicate":{}},{"condition":"minecraft:entity_properties","entity":"killer_player","predicate":{}},
                  {"condition":"minecraft:entity_scores","entity":"this","scores":{}}],
                  "entries":[{"type":"minecraft:item","name":"minecraft:bone","functions":[{"function":"minecraft:set_name","entity":"killer","name":"x"}]}]}]}""",
                "data/m/loot_table/entities/e.json");
        JsonObject pool = r.getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject();
        assertEquals("attacker", pool.getAsJsonArray("conditions").get(0).getAsJsonObject().get("entity").getAsString());
        assertEquals("direct_attacker", pool.getAsJsonArray("conditions").get(1).getAsJsonObject().get("entity").getAsString());
        assertEquals("attacking_player", pool.getAsJsonArray("conditions").get(2).getAsJsonObject().get("entity").getAsString());
        assertEquals("this", pool.getAsJsonArray("conditions").get(3).getAsJsonObject().get("entity").getAsString());
        assertEquals("attacker", pool.getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("functions").get(0).getAsJsonObject().get("entity").getAsString());

        JsonElement predicate = one("data/m/predicates/k.json", "{\"condition\":\"minecraft:entity_properties\",\"entity\":\"killer_player\",\"predicate\":{}}", "data/m/predicate/k.json");
        assertEquals("attacking_player", predicate.getAsJsonObject().get("entity").getAsString());
    }

    @Test
    void conventionTagPairsComeFromNeoForgesOwnTagDefinitions() {
        assertEquals("c:bricks/normal", DataConverter1201.mapTag("forge:ingots/brick"));
        assertEquals("#c:bricks/nether", DataConverter1201.mapTag("#forge:ingots/nether_brick"));
        assertEquals("c:glass_blocks/cheap", DataConverter1201.mapTag("forge:glass_silica"));
        assertEquals("c:glass_blocks/colorless", DataConverter1201.mapTag("forge:glass_colorless"));
        assertEquals("c:sandstone/blocks", DataConverter1201.mapTag("forge:sandstone"));
        assertEquals("c:relocation_not_supported", DataConverter1201.mapTag("forge:immovable"));
        assertEquals("c:tools/brush", DataConverter1201.mapTag("forge:tools/brushes"));
        assertEquals("c:is_dense_vegetation/overworld", DataConverter1201.mapTag("forge:is_dense/overworld"));
        assertEquals("c:is_aquatic", DataConverter1201.mapTag("forge:is_water"));
        // the segment rules still apply to the rest, and the pairs agree with them where both know a tag
        assertEquals("c:stones", DataConverter1201.mapTag("forge:stone"));
        assertEquals("c:cobblestones/mossy", DataConverter1201.mapTag("forge:cobblestone/mossy"));
        assertEquals("c:ingots/iron", DataConverter1201.mapTag("forge:ingots/iron"));
        assertEquals("data/c/tags/item/bricks/normal.json", DataConverter1201.mapPath("data/forge/tags/items/ingots/brick.json"));
        assertEquals("data/c/tags/worldgen/biome/is_dense_vegetation.json", DataConverter1201.mapPath("data/forge/tags/worldgen/biome/is_dense.json"));
        assertEquals("data/c/tags/worldgen/biome/is_cold.json", DataConverter1201.mapPath("data/forge/tags/worldgen/biome/is_cold.json"));
    }

    @Test
    void everyPairTargetIsAConventionTagNeoForgeShips() throws Exception {
        java.util.Set<String> known = new java.util.HashSet<>();
        try (var in = DataConverter1201Test.class.getResourceAsStream("/legacyconnector/forward/1.20.1/c-tags.txt")) {
            for (String l : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) if (!l.isBlank()) known.add(l.trim());
        }
        int rows = 0;
        try (var in = DataConverter1201Test.class.getResourceAsStream("/legacyconnector/forward/1.20.1/forge-c-tag-pairs.tsv")) {
            for (String l : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (l.isBlank() || l.startsWith("#")) continue;
                String[] a = l.split("\t");
                assertEquals(2, a.length, l);
                rows++;
                boolean found = false;
                for (String registry : List.of("item", "block", "fluid", "worldgen/biome")) found |= known.contains(registry + "/" + a[1]);
                assertTrue(found, "no c: tag " + a[1]);
            }
        }
        assertTrue(rows >= 40, "rows: " + rows);
    }

    @Test
    void camelCaseAndHorseAttributeIdsAreMigrated() {
        JsonElement r = one("data/m/loot_tables/blocks/a.json", """
                {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"minecraft:stick","functions":[
                  {"function":"minecraft:set_attributes","modifiers":[{"attribute":"generic.maxHealth","amount":1,"operation":"addition","name":"a","slot":"mainhand"},
                                                                       {"attribute":"horse.jump_strength","amount":1,"operation":"addition","name":"b","slot":"mainhand"}]}]}]}]}""",
                "data/m/loot_table/blocks/a.json");
        JsonArray mods = r.getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject()
                .getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonArray("modifiers");
        assertEquals("minecraft:generic.max_health", mods.get(0).getAsJsonObject().get("attribute").getAsString());
        assertEquals("minecraft:generic.jump_strength", mods.get(1).getAsJsonObject().get("attribute").getAsString());
    }

    @Test
    void snbtRoundTripKeepsTypes() {
        Snbt1201.Compound c = Snbt1201.parseCompound("{a:1b,b:2s,c:3,d:4L,e:1.5f,f:2.5d,g:\"x\\\"y\",h:[1,2],i:[I;1,2],j:{k:true},'q r':1}");
        assertEquals("{a:1b,b:2s,c:3,d:4L,e:1.5f,f:2.5d,g:\"x\\\"y\",h:[1,2],i:[I;1,2],j:{k:1b},\"q r\":1}", Snbt1201.write(c));
        assertEquals(null, Snbt1201.parseCompound("{a:1"));
        assertEquals(null, Snbt1201.parseCompound("[1,2]"));
        // a JSON object is SNBT too (Forge accepted it as the nbt of a result)
        Snbt1201.Compound json = Snbt1201.parseField(JsonParser.parseString("{\"Damage\":3,\"display\":{\"Name\":\"\\\"n\\\"\"}}"));
        assertEquals(3L, ((Snbt1201.Num) json.get("Damage")).asLong());
    }
}
