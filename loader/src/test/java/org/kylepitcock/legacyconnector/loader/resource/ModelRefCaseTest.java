package org.kylepitcock.legacyconnector.loader.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ModelRefCaseTest {
    private static JsonObject fix(String json) {
        return JsonParser.parseString(new String(LegacyResourceConverter.lowercaseResourceRefs(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void rootParentAndTexturesAreLowerCased() {
        JsonObject o = fix("{\"parent\":\"MyMod:Block/Cube\",\"textures\":{\"all\":\"MyMod:Blocks/Stone\"}}");
        assertEquals("mymod:block/cube", o.get("parent").getAsString());
        assertEquals("mymod:blocks/stone", o.getAsJsonObject("textures").get("all").getAsString());
    }

    @Test
    void boneParentsOfNonMinecraftModelFormatsKeepTheirCase() {
        // HuajiAge's stand models (Bedrock geometry) live under models/ and name a bone as "parent"
        JsonObject o = fix("{\"geometry.model\":{\"bones\":[{\"name\":\"bikeHead\"},{\"name\":\"hb1\",\"parent\":\"bikeHead\"}]}}");
        var bones = o.getAsJsonObject("geometry.model").getAsJsonArray("bones");
        assertEquals("bikeHead", bones.get(1).getAsJsonObject().get("parent").getAsString());
    }
}
