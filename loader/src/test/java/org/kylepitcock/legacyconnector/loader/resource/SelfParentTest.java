package org.kylepitcock.legacyconnector.loader.resource;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SelfParentTest {
    private static String parent(String json, String path) {
        byte[] out = LegacyResourceConverter.qualifySelfParent(json.getBytes(StandardCharsets.UTF_8), path);
        return JsonParser.parseString(new String(out, StandardCharsets.UTF_8)).getAsJsonObject().get("parent").getAsString();
    }

    @Test
    void aModelWhoseBareParentIsItselfPointsAtVanilla() {
        assertEquals("minecraft:item/generated", parent("{\"parent\":\"item/generated\",\"display\":{}}", "assets/thedalekmod/models/item/generated.json"));
    }

    @Test
    void anOverrideThatNamesItsOwnModelIsDropped() {
        String json = "{\"parent\":\"item/generated\",\"overrides\":[{\"predicate\":{\"ext\":0},\"model\":\"thedalekmod:item/sonic_ext\"},{\"predicate\":{\"ext\":1},\"model\":\"thedalekmod:item/sonic\"}]}";
        byte[] out = LegacyResourceConverter.qualifySelfParent(json.getBytes(StandardCharsets.UTF_8), "assets/thedalekmod/models/item/sonic.json");
        var overrides = JsonParser.parseString(new String(out, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("overrides");
        assertEquals(1, overrides.size());
        assertEquals("thedalekmod:item/sonic_ext", overrides.get(0).getAsJsonObject().get("model").getAsString());
    }

    @Test
    void overridesThatLeadBackToTheModelTheyWereReachedFromAreDropped() {
        java.util.Map<String, byte[]> files = new java.util.LinkedHashMap<>();
        files.put("assets/m/models/item/cane.json", "{\"overrides\":[{\"predicate\":{\"open\":1},\"model\":\"m:item/cane_open\"}]}".getBytes(StandardCharsets.UTF_8));
        files.put("assets/m/models/item/cane_open.json", "{\"overrides\":[{\"predicate\":{\"open\":0},\"model\":\"m:item/cane\"},{\"predicate\":{\"x\":1},\"model\":\"m:item/other\"}]}".getBytes(StandardCharsets.UTF_8));
        files.put("assets/m/models/item/other.json", "{\"overrides\":[]}".getBytes(StandardCharsets.UTF_8));
        LegacyResourceConverter.breakOverrideCycles(files);
        var cane = JsonParser.parseString(new String(files.get("assets/m/models/item/cane.json"), StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("overrides");
        var open = JsonParser.parseString(new String(files.get("assets/m/models/item/cane_open.json"), StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("overrides");
        assertEquals(1, cane.size());
        assertEquals(1, open.size());
        assertEquals("m:item/other", open.get(0).getAsJsonObject().get("model").getAsString());
    }

    @Test
    void otherParentsStayAsTheyAre() {
        assertEquals("item/generated", parent("{\"parent\":\"item/generated\"}", "assets/thedalekmod/models/item/chair.json"));
        assertEquals("thedalekmod:item/generated", parent("{\"parent\":\"thedalekmod:item/generated\"}", "assets/thedalekmod/models/item/generated.json"));
    }
}
