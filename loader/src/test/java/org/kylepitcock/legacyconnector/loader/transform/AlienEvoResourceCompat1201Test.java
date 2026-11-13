package org.kylepitcock.legacyconnector.loader.transform;

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
}
