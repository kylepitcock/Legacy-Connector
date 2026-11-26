package org.kylepitcock.legacyconnector.loader.transform;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AlienEvoResourceCompat1201Test {
    private static final String TEST_POWER = """
            {
              "hidden": true,
              "abilities": {
                "generate_code": {"type":"alienevo:generate_random"},
                "check_code": {"type":"alienevo:check_code"},
                "display_codes": {"type":"alienevo:display_code"}
              }
            }
            """;


    private static final String UNSHADED_VERTEX = """
            #version 150
            #moj_import <fog.glsl>
            in vec3 Position;
            uniform mat4 ModelViewMat;
            uniform mat4 ProjMat;
            uniform mat3 IViewRotMat;
            uniform int FogShape;
            out float vertexDistance;
            void main() {
                gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);
            }
            """;

    private static final String UNSHADED_JSON = """
            {
              "vertex": "alienevo_rendertype_entity_unshaded",
              "uniforms": [
                {"name":"ModelViewMat","type":"matrix4x4","count":16,"values":[]},
                {"name":"IViewRotMat","type":"matrix3x3","count":9,"values":[]},
                {"name":"FogShape","type":"int","count":1,"values":[0]}
              ]
            }
            """;

    private static Map<String, byte[]> unshadedShaderResources() {
        Map<String, byte[]> resources = new LinkedHashMap<>();
        resources.put(AlienEvoResourceCompat1201.UNSHADED_ENTITY_SHADER_JSON,
                UNSHADED_JSON.getBytes(StandardCharsets.UTF_8));
        resources.put(AlienEvoResourceCompat1201.UNSHADED_ENTITY_SHADER_VERTEX,
                UNSHADED_VERTEX.getBytes(StandardCharsets.UTF_8));
        return resources;
    }

    private static Map<String, byte[]> resources(String json) {
        Map<String, byte[]> resources = new LinkedHashMap<>();
        resources.put(AlienEvoResourceCompat1201.ORPHAN_CODE_POWER,
                json.getBytes(StandardCharsets.UTF_8));
        return resources;
    }

    @Test
    void dropsOnlyTheUnreferencedHiddenTestPower() {
        Map<String, byte[]> resources = resources(TEST_POWER);

        assertTrue(AlienEvoResourceCompat1201.removeUnreferencedHiddenCodePower(resources, Map.of()));
        assertFalse(resources.containsKey(AlienEvoResourceCompat1201.ORPHAN_CODE_POWER));
    }

    @Test
    void preservesThePowerWhenAnyResourceReferencesItOrItsMissingAbilityTypes() {
        Map<String, byte[]> powerReference = resources(TEST_POWER);
        powerReference.put("assets/alienevo/kubejs_scripts/other.js",
                "let id = 'alienevo:code_power_test';".getBytes(StandardCharsets.UTF_8));
        assertFalse(AlienEvoResourceCompat1201.removeUnreferencedHiddenCodePower(powerReference, Map.of()));
        assertTrue(powerReference.containsKey(AlienEvoResourceCompat1201.ORPHAN_CODE_POWER));

        Map<String, byte[]> abilityReference = resources(TEST_POWER);
        abilityReference.put("addon/alienevo/kubejs_scripts/other.js",
                "event.create('alienevo:generate_random');".getBytes(StandardCharsets.UTF_8));
        assertFalse(AlienEvoResourceCompat1201.removeUnreferencedHiddenCodePower(abilityReference, Map.of()));

        Map<String, byte[]> pathReference = resources(TEST_POWER);
        pathReference.put("data/alienevo/palladium/powers/code_power_test_extension.json",
                "{}".getBytes(StandardCharsets.UTF_8));
        assertFalse(AlienEvoResourceCompat1201.removeUnreferencedHiddenCodePower(pathReference, Map.of()));
    }

    @Test
    void preservesVisibleAndUnrecognizedPowerResources() {
        Map<String, byte[]> visible = resources(TEST_POWER.replace("\"hidden\": true", "\"hidden\": false"));
        assertFalse(AlienEvoResourceCompat1201.removeUnreferencedHiddenCodePower(visible, Map.of()));

        Map<String, byte[]> changed = resources(TEST_POWER.replace("alienevo:check_code", "alienevo:registered_ability"));
        assertFalse(AlienEvoResourceCompat1201.removeUnreferencedHiddenCodePower(changed, Map.of()));

        Map<String, byte[]> malformed = resources("{not json");
        assertFalse(AlienEvoResourceCompat1201.removeUnreferencedHiddenCodePower(malformed, Map.of()));
    }

    @Test
    void makesOnlyTheAdAstraDamageReferenceOptional() {
        Map<String, byte[]> resources = new LinkedHashMap<>();
        resources.put(AlienEvoResourceCompat1201.EVOASTRA_DAMAGE_TAG, """
                {"replace":false,"values":["ad_astra:oxygen","minecraft:is_fall"]}
                """.getBytes(StandardCharsets.UTF_8));
        assertTrue(AlienEvoResourceCompat1201.makeAdAstraDamageTypeOptional(resources));
        String migrated = new String(resources.get(AlienEvoResourceCompat1201.EVOASTRA_DAMAGE_TAG),
                StandardCharsets.UTF_8);
        String quote = String.valueOf((char)34);
        assertTrue(migrated.contains(quote + "id" + quote + ":" + quote + "ad_astra:oxygen" + quote), migrated);
        assertTrue(migrated.contains(quote + "required" + quote + ":false"), migrated);
        assertTrue(migrated.contains(quote + "minecraft:is_fall" + quote), migrated);
        assertFalse(AlienEvoResourceCompat1201.makeAdAstraDamageTypeOptional(resources));
    }


    @Test
    void removesOnlyTheMalformedKineceleranKickStopPositionChannel() {
        Map<String, byte[]> resources = new LinkedHashMap<>();
        resources.put(AlienEvoResourceCompat1201.KINECELERAN_ANIMATION, """
                {
                  "animations": {
                    "animation.kineceleran.kick_stop": {
                      "bones": {
                        "armorHead": {
                          "rotation": {"0.0":{"vector":[45,15,-5],"easing":"easeInElastic"}},
                          "position": {"vector":["easeInElastic","easeInElastic","easeInElastic"]}
                        }
                      }
                    }
                  }
                }
                """.getBytes(StandardCharsets.UTF_8));

        assertTrue(AlienEvoResourceCompat1201.repairKineceleranKickStopPositionVector(resources));
        String repaired = new String(resources.get(AlienEvoResourceCompat1201.KINECELERAN_ANIMATION),
                StandardCharsets.UTF_8);
        assertTrue(repaired.contains("\"rotation\""), repaired);
        assertTrue(repaired.contains("\"easeInElastic\""), repaired);
        assertFalse(repaired.contains("\"position\""), repaired);
        assertFalse(AlienEvoResourceCompat1201.repairKineceleranKickStopPositionVector(resources));
    }

    @Test
    void leavesValidEasingFieldsAndOtherMalformedShapesUntouched() {
        Map<String, byte[]> resources = new LinkedHashMap<>();
        resources.put(AlienEvoResourceCompat1201.KINECELERAN_ANIMATION, """
                {
                  "animations": {
                    "animation.kineceleran.kick_stop": {
                      "bones": {
                        "armorHead": {
                          "position": {"vector":[0,0,0],"easing":"easeInElastic"}
                        }
                      }
                    }
                  }
                }
                """.getBytes(StandardCharsets.UTF_8));
        assertFalse(AlienEvoResourceCompat1201.repairKineceleranKickStopPositionVector(resources));

        Map<String, byte[]> wrongKeyframe = new LinkedHashMap<>();
        wrongKeyframe.put(AlienEvoResourceCompat1201.KINECELERAN_ANIMATION, """
                {
                  "animations": {
                    "animation.kineceleran.kick_stop": {
                      "bones": {
                        "armorHead": {
                          "position": {"vector":["easeInElastic","easeInElastic",0]}
                        }
                      }
                    }
                  }
                }
                """.getBytes(StandardCharsets.UTF_8));
        assertFalse(AlienEvoResourceCompat1201.repairKineceleranKickStopPositionVector(wrongKeyframe));
    }

    @Test
    void addsTheVanillaAddonRootMarkerOnlyWhenAddonResourcesExist() {
        Map<String, byte[]> resources = new LinkedHashMap<>();
        resources.put("addon/alienevo/textures/example.png", new byte[] {1});
        assertTrue(AlienEvoResourceCompat1201.ensureAddonResourceRootMarker(resources));
        assertTrue(resources.containsKey(AlienEvoResourceCompat1201.ADDON_RESOURCE_ROOT_MARKER));
        assertFalse(AlienEvoResourceCompat1201.ensureAddonResourceRootMarker(resources));
    }

    @Test
    void doesNotCreateAnAddonMarkerForOrdinaryAssetsOrAnEmptyResourceSet() {
        Map<String, byte[]> resources = new LinkedHashMap<>();
        resources.put("assets/alienevo/textures/example.png", new byte[] {1});
        assertFalse(AlienEvoResourceCompat1201.ensureAddonResourceRootMarker(resources));
        assertFalse(resources.containsKey(AlienEvoResourceCompat1201.ADDON_RESOURCE_ROOT_MARKER));
    }

    @Test
    void leavesAnUnrecognizedOrMissingAdAstraTagAlone() {
        Map<String, byte[]> missing = new LinkedHashMap<>();
        assertFalse(AlienEvoResourceCompat1201.makeAdAstraDamageTypeOptional(missing));
        Map<String, byte[]> different = new LinkedHashMap<>();
        String quote = String.valueOf((char)34);
        different.put(AlienEvoResourceCompat1201.EVOASTRA_DAMAGE_TAG,
                ("{" + quote + "values" + quote + ":[" + quote + "example:other" + quote + "]}")
                        .getBytes(StandardCharsets.UTF_8));
        assertFalse(AlienEvoResourceCompat1201.makeAdAstraDamageTypeOptional(different));
    }

    @Test
    void preservesThePowerWhenAClassReferencesTheMissingType() {
        Map<String, byte[]> resources = resources(TEST_POWER);
        Map<String, byte[]> classes = Map.of(
                "com/example/Power.class",
                "alienevo:check_code".getBytes(StandardCharsets.UTF_8));
        assertFalse(AlienEvoResourceCompat1201.removeUnreferencedHiddenCodePower(resources, classes));
    }

    @Test
    void migratesTheUnshadedEntityShaderToThe121FogDistanceContract() {
        Map<String, byte[]> resources = unshadedShaderResources();

        assertTrue(AlienEvoResourceCompat1201.repairUnshadedEntityShaderFor121(resources));

        String vertex = new String(resources.get(AlienEvoResourceCompat1201.UNSHADED_ENTITY_SHADER_VERTEX),
                StandardCharsets.UTF_8);
        assertTrue(vertex.contains("vertexDistance = fog_distance(Position, FogShape);"), vertex);
        assertFalse(vertex.contains("IViewRotMat"), vertex);
        assertFalse(vertex.contains("fog_distance(ModelViewMat"), vertex);

        String json = new String(resources.get(AlienEvoResourceCompat1201.UNSHADED_ENTITY_SHADER_JSON),
                StandardCharsets.UTF_8);
        assertFalse(json.contains("IViewRotMat"), json);
        assertTrue(json.contains("ModelViewMat"), json);
        assertTrue(json.contains("FogShape"), json);

        assertFalse(AlienEvoResourceCompat1201.repairUnshadedEntityShaderFor121(resources));
    }

    @Test
    void leavesMissingOrUnexpectedUnshadedShaderAssetsUntouched() {
        Map<String, byte[]> missing = new LinkedHashMap<>();
        assertFalse(AlienEvoResourceCompat1201.repairUnshadedEntityShaderFor121(missing));

        Map<String, byte[]> changedVertex = unshadedShaderResources();
        byte[] originalJson = changedVertex.get(AlienEvoResourceCompat1201.UNSHADED_ENTITY_SHADER_JSON).clone();
        byte[] unexpected = UNSHADED_VERTEX.replace(
                "fog_distance(ModelViewMat, IViewRotMat * Position, FogShape)",
                "length(Position)").getBytes(StandardCharsets.UTF_8);
        changedVertex.put(AlienEvoResourceCompat1201.UNSHADED_ENTITY_SHADER_VERTEX, unexpected);
        assertFalse(AlienEvoResourceCompat1201.repairUnshadedEntityShaderFor121(changedVertex));
        assertTrue(java.util.Arrays.equals(originalJson,
                changedVertex.get(AlienEvoResourceCompat1201.UNSHADED_ENTITY_SHADER_JSON)));

        Map<String, byte[]> extraReference = unshadedShaderResources();
        extraReference.put(AlienEvoResourceCompat1201.UNSHADED_ENTITY_SHADER_VERTEX,
                (UNSHADED_VERTEX + "\n// IViewRotMat must not be used elsewhere").getBytes(StandardCharsets.UTF_8));
        assertFalse(AlienEvoResourceCompat1201.repairUnshadedEntityShaderFor121(extraReference));
    }

    @Test
    void doesNotPartiallyRewriteWhenTheShaderJsonLayoutIsUnexpected() {
        Map<String, byte[]> malformed = unshadedShaderResources();
        String originalVertex = new String(
                malformed.get(AlienEvoResourceCompat1201.UNSHADED_ENTITY_SHADER_VERTEX), StandardCharsets.UTF_8);
        malformed.put(AlienEvoResourceCompat1201.UNSHADED_ENTITY_SHADER_JSON,
                "{not json".getBytes(StandardCharsets.UTF_8));
        assertFalse(AlienEvoResourceCompat1201.repairUnshadedEntityShaderFor121(malformed));
        assertTrue(originalVertex.equals(new String(
                malformed.get(AlienEvoResourceCompat1201.UNSHADED_ENTITY_SHADER_VERTEX), StandardCharsets.UTF_8)));

        Map<String, byte[]> missingUniform = unshadedShaderResources();
        String jsonWithoutViewUniform = UNSHADED_JSON.replace(
                "{\"name\":\"IViewRotMat\",\"type\":\"matrix3x3\",\"count\":9,\"values\":[]},", "");
        missingUniform.put(AlienEvoResourceCompat1201.UNSHADED_ENTITY_SHADER_JSON,
                jsonWithoutViewUniform.getBytes(StandardCharsets.UTF_8));
        assertFalse(AlienEvoResourceCompat1201.repairUnshadedEntityShaderFor121(missingUniform));

        Map<String, byte[]> wrongUniformType = unshadedShaderResources();
        wrongUniformType.put(AlienEvoResourceCompat1201.UNSHADED_ENTITY_SHADER_JSON,
                UNSHADED_JSON.replace("matrix3x3", "matrix4x4").getBytes(StandardCharsets.UTF_8));
        assertFalse(AlienEvoResourceCompat1201.repairUnshadedEntityShaderFor121(wrongUniformType));

        Map<String, byte[]> duplicateUniform = unshadedShaderResources();
        String duplicated = UNSHADED_JSON.replace(
                "{\"name\":\"FogShape\"",
                "{\"name\":\"IViewRotMat\",\"type\":\"matrix3x3\",\"count\":9,\"values\":[]},"
                        + "{\"name\":\"FogShape\"");
        duplicateUniform.put(AlienEvoResourceCompat1201.UNSHADED_ENTITY_SHADER_JSON,
                duplicated.getBytes(StandardCharsets.UTF_8));
        assertFalse(AlienEvoResourceCompat1201.repairUnshadedEntityShaderFor121(duplicateUniform));
    }


    @Test
    void repairsTheObservedDev24ResourceParseErrorsAndIsIdempotent() {
        Map<String, byte[]> resources = new LinkedHashMap<>();
        resources.put("data/alienevo/loot_tables/archaeology/codes_rare.json", """
                {"pools":[{"rolls":1.0,"entries":[{"type":"minecraft:item","name":"alienevo:code_tablet"}]}]}
                """.getBytes(StandardCharsets.UTF_8));
        resources.put("data/alienevo/tags/items/prototype_cores.json", """
                {"replace":false,"values":["alienevo:prototype_core"]}
                """.getBytes(StandardCharsets.UTF_8));
        resources.put("data/alienevo/recipes/prototype_omnitrix.json", """
                {"type":"alienevo:core_omnitrix","result":{"item":"alienevo:prototype_omnitrix","count":1}}
                """.getBytes(StandardCharsets.UTF_8));
        resources.put("assets/alienevo/palladium/render_layers/aliens/alien_34/aerophibian.json", """
                {"type":"palladium:compound","layers":[
                  {"type":"geckolib:default","conditions":[{"type":"alienevo:watch_type","value":"prototype"}]},
                  {"type":"geckolib:default","conditions":[{"type":"alienevo:watch_type","value":"recal"}]},
                  {"type":"geckolib:default","conditions":[{"type":"alienevo:watch_type","value":"recal"}]}
                ]}
                """.getBytes(StandardCharsets.UTF_8));
        resources.put("data/alienevo_aliens/palladium/powers/dragonoid.json", """
                {"abilities":{
                  "screen_shake":{"type":"alienevo:screenshake"},
                  "infinity":{"type":"alienevo:infinity"}
                }}
                """.getBytes(StandardCharsets.UTF_8));
        resources.put("data/alienevo_aliens/palladium/powers/vaxasaurian.json", """
                {"abilities":{
                  "punch":{"type":"palladium:animation_timer","conditions":{"enabling":[{"type":"afomni:is_mainhand"}]}},
                  "leap":{"type":"afomni:vax_leap"},
                  "tail_spin_damage":{"type":"afomni:aoe_damage"}
                }}
                """.getBytes(StandardCharsets.UTF_8));
        resources.put("data/alienevo_aliens/palladium/powers/astrobot.json", """
                {"abilities":{
                  "laser_jump":{"type":"alienevo:astrojump"},
                  "astro_laser_damage":{"type":"alienevo:astro_laser_damage"},
                  "punch_ability_ground":{"type":"alienevo:astro_punch_damage"},
                  "punch_ability_air":{"type":"alienevo:astro_punch_air_damage"},
                  "testanim":{"type":"alienevo:astro_chargepunch"},
                  "astrobot_loop":{"type":"palladium:command"}
                }}
                """.getBytes(StandardCharsets.UTF_8));
        resources.put("data/alienevo_aliens/palladium/powers/kryptonian.json", """
                {"abilities":{
                  "punch":{"type":"palladium:animation_timer","conditions":{"enabling":[{"type":"afomni:is_mainhand"}]}},
                  "flight_damage":{"type":"afomni:aoe_damage"}
                }}
                """.getBytes(StandardCharsets.UTF_8));
        resources.put("data/alienevo/palladium/powers/galaxy_aurora.json", """
                {"abilities":{
                  "infinity":{"type":"alienevo:infinity"},
                  "tetramand_spin_damage":{"type":"afomni:aoe_damage"},
                  "heal_regen":{"type":"alienevo:entity_damage","effect":"minecraft:regeneration","conditions":{"enabling":[{"type":"palladium:ability_enabled","ability":"heal"}]}}
                }}
                """.getBytes(StandardCharsets.UTF_8));
        resources.put("data/afomni/functions/creation/upgrade_fade.mcfunction",
                "execute if block ~ ~ ~ afomni:packed_ice run setblock ~ ~ ~ afomni:blue_ice\n".getBytes(StandardCharsets.UTF_8));
        resources.put("data/afomni/functions/creation/place_ice_fade.mcfunction",
                "execute if block ~ ~ ~ afomni:ice run setblock ~ ~ ~ afomni:blue_ice\n".getBytes(StandardCharsets.UTF_8));
        resources.put("data/alienevo/functions/abilities/stack_ice.mcfunction",
                "execute if block ~ ~ ~ alienevo:ice14 run setblock ~ ~ ~ alienevo:ice_block replace\n"
                        .getBytes(StandardCharsets.UTF_8));
        resources.put("data/alienevo/functions/abilities/pyronite_nova.mcfunction",
                "execute if score @s Pyronite.Nova matches 35 run fireparticle ^ ^2 ^ 6500 0 0 0 0.3\n"
                        + "execute if score @s Pyronite.Nova matches 40 run energybar value set @s alienevo_aliens:pyronite chroma_energy 0\n"
                        .getBytes(StandardCharsets.UTF_8));

        assertEquals(13, AlienEvoResourceCompat1201.repairDev24ResourceErrors(resources));

        JsonObject loot = parse(resources, "data/alienevo/loot_tables/archaeology/codes_rare.json");
        JsonArray entries = loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        assertEquals(4, entries.size());
        assertTrue(entries.toString().contains("code_tablet_mastercontrol_on"));
        assertTrue(entries.toString().contains("code_tablet_selfdestruct_off"));

        JsonObject tag = parse(resources, "data/alienevo/tags/items/prototype_cores.json");
        assertEquals("alienevo:prototype_omnitrix_core",
                tag.getAsJsonArray("values").get(0).getAsString());

        JsonObject recipe = parse(resources, "data/alienevo/recipes/prototype_omnitrix.json");
        assertEquals("alienevo:prototype_omnitrix",
                recipe.getAsJsonObject("result").get("id").getAsString());
        assertFalse(recipe.getAsJsonObject("result").has("item"));

        JsonObject layer = parse(resources,
                "assets/alienevo/palladium/render_layers/aliens/alien_34/aerophibian.json");
        JsonArray layers = layer.getAsJsonArray("layers");
        assertFalse(layers.get(0).getAsJsonObject().has("conditions"));
        assertEquals("alienevo:watch_state", layers.get(1).getAsJsonObject()
                .getAsJsonArray("conditions").get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("RECALIBRATED", layers.get(2).getAsJsonObject()
                .getAsJsonArray("conditions").get(0).getAsJsonObject().get("value").getAsString());

        JsonObject dragonoid = parse(resources, "data/alienevo_aliens/palladium/powers/dragonoid.json");
        assertFalse(dragonoid.getAsJsonObject("abilities").has("infinity"));
        assertEquals("alienevo:screen_shake", dragonoid.getAsJsonObject("abilities")
                .getAsJsonObject("screen_shake").get("type").getAsString());

        JsonObject vaxasaurian = parse(resources, "data/alienevo_aliens/palladium/powers/vaxasaurian.json");
        assertFalse(vaxasaurian.getAsJsonObject("abilities").has("leap"));
        assertEquals("alienevo:is_mainhand", vaxasaurian.getAsJsonObject("abilities")
                .getAsJsonObject("punch").getAsJsonObject("conditions").getAsJsonArray("enabling")
                .get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("alienevo:aoe_damage", vaxasaurian.getAsJsonObject("abilities")
                .getAsJsonObject("tail_spin_damage").get("type").getAsString());

        JsonObject astrobot = parse(resources, "data/alienevo_aliens/palladium/powers/astrobot.json");
        assertEquals(1, astrobot.getAsJsonObject("abilities").size());
        assertTrue(astrobot.getAsJsonObject("abilities").has("astrobot_loop"));

        JsonObject kryptonian = parse(resources, "data/alienevo_aliens/palladium/powers/kryptonian.json");
        assertEquals("alienevo:is_mainhand", kryptonian.getAsJsonObject("abilities")
                .getAsJsonObject("punch").getAsJsonObject("conditions").getAsJsonArray("enabling")
                .get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("alienevo:aoe_damage", kryptonian.getAsJsonObject("abilities")
                .getAsJsonObject("flight_damage").get("type").getAsString());

        JsonObject aurora = parse(resources, "data/alienevo/palladium/powers/galaxy_aurora.json");
        assertFalse(aurora.getAsJsonObject("abilities").has("infinity"));
        assertEquals("palladium:command", aurora.getAsJsonObject("abilities")
                .getAsJsonObject("heal_regen").get("type").getAsString());
        assertTrue(aurora.getAsJsonObject("abilities").getAsJsonObject("heal_regen")
                .getAsJsonArray("commands").get(0).getAsString().contains("minecraft:regeneration"));

        assertTrue(text(resources, "data/afomni/functions/creation/upgrade_fade.mcfunction")
                .contains("alienevo:packed_ice_layer"));
        assertTrue(text(resources, "data/afomni/functions/creation/place_ice_fade.mcfunction")
                .contains("alienevo:ice_layer"));
        assertTrue(text(resources, "data/alienevo/functions/abilities/stack_ice.mcfunction")
                .contains("alienevo:ice_layer[layers=7]"));
        assertTrue(text(resources, "data/alienevo/functions/abilities/stack_ice.mcfunction")
                .contains("alienevo:ice_layer[layers=8]"));
        String pyronite = text(resources, "data/alienevo/functions/abilities/pyronite_nova.mcfunction");
        assertTrue(pyronite.contains("particle minecraft:flame"));
        assertTrue(pyronite.contains("alienevo energy set alienevo_aliens:pyronite 0 @s"));

        assertEquals(0, AlienEvoResourceCompat1201.repairDev24ResourceErrors(resources));
    }

    private static JsonObject parse(Map<String, byte[]> resources, String path) {
        return JsonParser.parseString(text(resources, path)).getAsJsonObject();
    }

    private static String text(Map<String, byte[]> resources, String path) {
        return new String(resources.get(path), StandardCharsets.UTF_8);
    }

}
