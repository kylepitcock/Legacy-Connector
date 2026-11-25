package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Regression coverage for AlienEvo dev.24 Cannonbolt roll-bone hierarchy repair. */
class PalladiumResourceCompat1201CanonboltTest {

    @Test
    void repairsCanonboltRollBoneHierarchyAcrossAllUniformGeometries() {
        Map<String, byte[]> resources = new HashMap<>();
        String animation = """
                {
                  "animations": {
                    "animation.arburian_pelarota.ball_in": {
                      "bones": {"armorBall": {"rotation": {
                        "0.6667": {"vector": ["90 + alienevo.roll_since(0.5833)", 0, 0]}
                      }}}
                    },
                    "animation.arburian_pelarota.ball_out": {
                      "bones": {"armorBall": {"rotation": {
                        "0.0": {"vector": ["90 + alienevo.roll_since(0)", 0, 0]}
                      }}}
                    },
                    "animation.arburian_pelarota.ball_state": {
                      "bones": {"armorBall": {"rotation": {
                        "vector": ["90 + alienevo.roll_since(0)", 0, 0]
                      }}}
                    }
                  }
                }
                """;
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_CANONBOLT_ANIMATION, animation);
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_CANONBOLT_GEOMETRIES.get(0),
                geometry("chest_upper", false, true));
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_CANONBOLT_GEOMETRIES.get(1),
                geometry("chest_upper", false, true));
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_CANONBOLT_GEOMETRIES.get(2),
                geometry("armorBody", false, false));
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_CANONBOLT_GEOMETRIES.get(3),
                geometry("chest_upper", true, true));

        assertEquals(4, PalladiumResourceCompat1201.apply(resources));

        for (int i = 0; i < PalladiumResourceCompat1201.ALIEN_EVO_CANONBOLT_GEOMETRIES.size(); i++) {
            String path = PalladiumResourceCompat1201.ALIEN_EVO_CANONBOLT_GEOMETRIES.get(i);
            JsonObject root = JsonParser.parseString(get(resources, path)).getAsJsonObject();
            JsonObject geometry = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonArray bones = geometry.getAsJsonArray("bones");
            JsonObject armorBall = findBone(bones, "armorBall");
            JsonObject ball = findBone(bones, "BALL");

            assertTrue(armorBall != null, path);
            assertTrue(ball != null, path);
            assertEquals(i == 2 ? "armorBody" : "chest_upper",
                    armorBall.get("parent").getAsString(), path);
            assertEquals("armorBall", ball.get("parent").getAsString(), path);
            assertEquals(ball.getAsJsonArray("pivot"), armorBall.getAsJsonArray("pivot"), path);
            assertTrue(boneIndex(bones, "armorBall") < boneIndex(bones, "BALL"), path);
            assertEquals(i != 2, ball.has("rotation"), path);
            assertEquals("BALL", findBone(bones, "ball_shell_front").get("parent").getAsString(), path);
        }

        assertEquals(animation, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_CANONBOLT_ANIMATION));
        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
    }

    @Test
    void leavesCanonboltGeometryUntouchedWhenTheOriginalRollTracksDoNotExist() {
        Map<String, byte[]> resources = new HashMap<>();
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_CANONBOLT_ANIMATION, """
                {"animations":{"animation.arburian_pelarota.ball_state":{"bones":{}}}}
                """);
        put(resources, PalladiumResourceCompat1201.ALIEN_EVO_CANONBOLT_GEOMETRIES.get(0),
                geometry("chest_upper", false, true));
        String original = get(resources, PalladiumResourceCompat1201.ALIEN_EVO_CANONBOLT_GEOMETRIES.get(0));

        assertEquals(0, PalladiumResourceCompat1201.apply(resources));
        assertEquals(original, get(resources, PalladiumResourceCompat1201.ALIEN_EVO_CANONBOLT_GEOMETRIES.get(0)));
    }

    private static void put(Map<String, byte[]> resources, String path, String content) {
        resources.put(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private static String get(Map<String, byte[]> resources, String path) {
        return new String(resources.get(path), StandardCharsets.UTF_8);
    }

    private static String geometry(String ballParent, boolean existingArmorBall, boolean hasBallRotation) {
        String armorBall = existingArmorBall
                ? "{\"name\":\"armorBall\",\"parent\":\"ROOT\",\"pivot\":[0,12.5,0]},"
                : "";
        String rotation = hasBallRotation ? ",\"rotation\":[-90,0,0]" : "";
        return """
                {
                  "format_version": "1.12.0",
                  "minecraft:geometry": [{
                    "description": {"identifier": "geometry.arburian_pelarota"},
                    "bones": [
                      {"name": "ROOT"},
                      %s
                      {"name": "chest_upper", "parent": "ROOT"},
                      {"name": "armorBody", "parent": "ROOT"},
                      {"name": "BALL", "parent": "%s", "pivot": [0,12.5,0]%s},
                      {"name": "ball_shell_front", "parent": "BALL"}
                    ]
                  }]
                }
                """.formatted(armorBall, ballParent, rotation);
    }

    private static JsonObject findBone(JsonArray bones, String name) {
        for (int i = 0; i < bones.size(); i++) {
            if (!bones.get(i).isJsonObject()) continue;
            JsonObject bone = bones.get(i).getAsJsonObject();
            if (bone.has("name") && name.equals(bone.get("name").getAsString())) return bone;
        }
        return null;
    }

    private static int boneIndex(JsonArray bones, String name) {
        for (int i = 0; i < bones.size(); i++) {
            if (bones.get(i).isJsonObject()
                    && bones.get(i).getAsJsonObject().has("name")
                    && name.equals(bones.get(i).getAsJsonObject().get("name").getAsString())) return i;
        }
        return -1;
    }
}
