package org.kylepitcock.legacyconnector.loader.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ForgeModelLoaderTest {
    private static String run(String json) {
        return new String(LegacyResourceConverter.renameForgeModelLoader(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    @Test
    void objLoaderIsRenamed() {
        String out = run("{\"loader\":\"forge:obj\",\"model\":\"m:models/block/x.obj\",\"textures\":{\"a\":\"m:blocks/t\"}}");
        assertTrue(out.contains("\"neoforge:obj\""), out);
        assertTrue(out.contains("m:models/block/x.obj"), out);
    }

    @Test
    void otherModelsAreUntouched() {
        String in = "{\"parent\":\"block/cube_all\",\"textures\":{\"all\":\"m:blocks/t\"}}";
        assertEquals(in, run(in));
        String other = "{\"loader\":\"neoforge:obj\"}";
        assertEquals(other, run(other));
    }
}
