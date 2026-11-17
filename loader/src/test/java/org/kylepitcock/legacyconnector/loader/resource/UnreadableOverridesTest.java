package org.kylepitcock.legacyconnector.loader.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class UnreadableOverridesTest {
    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static JsonObject run(String json) {
        byte[] out = LegacyResourceConverter.dropUnreadableOverrides(b(json), "assets/m/models/item/x.json");
        return out == null ? null : JsonParser.parseString(new String(out, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void tinkersBowOverridesWithTexturesButNoModelAreDropped() {
        JsonObject out = run("{\"textures\":{\"layer0\":\"m:items/a\"},\"overrides\":[{\"predicate\":{\"pulling\":1},\"textures\":{\"layer2\":\"m:items/b\"}}]}");
        assertFalse(out.has("overrides"));
        assertEquals("m:items/a", out.getAsJsonObject("textures").get("layer0").getAsString());
    }

    @Test
    void overridesThatAreAnObjectAreDropped() {
        assertFalse(run("{\"overrides\":{\"predicate\":{\"blocking\":1},\"display\":{}}}").has("overrides"));
    }

    @Test
    void entriesWithAModelStayAndOthersGo() {
        JsonObject out = run("{\"overrides\":[{\"predicate\":{\"a\":1},\"model\":\"m:item/y\"},{\"predicate\":{\"b\":1},\"textures\":{}}]}");
        assertEquals(1, out.getAsJsonArray("overrides").size());
        assertTrue(out.getAsJsonArray("overrides").get(0).getAsJsonObject().has("model"));
    }

    @Test
    void vanillaShapedOrAbsentOverridesAreLeftAlone() {
        assertNull(LegacyResourceConverter.dropUnreadableOverrides(b("{\"overrides\":[{\"predicate\":{\"a\":1},\"model\":\"m:item/y\"}]}"), "p"));
        assertNull(LegacyResourceConverter.dropUnreadableOverrides(b("{\"parent\":\"item/generated\"}"), "p"));
        assertNull(LegacyResourceConverter.dropUnreadableOverrides(b("not json"), "p"));
        assertNull(LegacyResourceConverter.dropUnreadableOverrides(b("[1,2]"), "p"));
    }
}
