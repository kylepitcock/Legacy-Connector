package org.kylepitcock.legacyconnector.loader.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** GeckoLib animation files with easing names where numbers belong (shaped like the Management Wanted animations). */
class GeckoAnimations1201Test {
    private static JsonElement convert(String json, DataConverter1201.Stats stats) {
        List<DataConverter1201.Out> outs = DataConverter1201.convert("assets/m/animations/a.animation.json", json.getBytes(StandardCharsets.UTF_8), stats);
        assertEquals(1, outs.size());
        assertEquals("assets/m/animations/a.animation.json", outs.get(0).path());
        return JsonParser.parseString(new String(outs.get(0).data(), StandardCharsets.UTF_8));
    }

    @Test
    void easingNamesInVectorsAndScalesBecomeZero() {
        DataConverter1201.Stats stats = new DataConverter1201.Stats();
        JsonElement out = convert("""
                {"format_version":"1.8.0","animations":{"a":{"bones":{"arm":{
                  "scale":"linear",
                  "position":{"0.0":{"vector":["linear","easeInOutSine","linear"],"easing":"linear"},"1.0":{"vector":[1,2,3],"easing":"easeInBounce","easingArgs":[0.2]}},
                  "rotation":{"0.5":["step",5,"math.sin(query.anim_time)"],"post":{"vector":["linear",0,0]}}}}}}}
                """, stats);
        JsonElement expected = JsonParser.parseString("""
                {"format_version":"1.8.0","animations":{"a":{"bones":{"arm":{
                  "scale":0,
                  "position":{"0.0":{"vector":[0,0,0],"easing":"linear"},"1.0":{"vector":[1,2,3],"easing":"easeInBounce","easingArgs":[0.2]}},
                  "rotation":{"0.5":[0,5,"math.sin(query.anim_time)"],"post":{"vector":[0,0,0]}}}}}}}
                """);
        assertEquals(expected, out);
        assertEquals(1, stats.counts.get("geckolib_easing_vector"));
    }

    @Test
    void validAnimationsAreKeptByteForByte() {
        String json = "{\"animations\":{\"a\":{\"bones\":{\"b\":{\"rotation\":{\"0.0\":{\"vector\":[0,1,2],\"easing\":\"linear\"}},\"scale\":\"math.cos(1)\"}}}}}";
        DataConverter1201.Stats stats = new DataConverter1201.Stats();
        List<DataConverter1201.Out> outs = DataConverter1201.convert("assets/m/animations/a.animation.json", json.getBytes(StandardCharsets.UTF_8), stats);
        assertEquals(json, new String(outs.get(0).data(), StandardCharsets.UTF_8));
        assertEquals(0, stats.total());
    }
}
