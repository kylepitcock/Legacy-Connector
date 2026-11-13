package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** Regression tests for the KubeJS 6 BlockBuilder aliases exposed on KubeJS 7. */
class KubeJSBlockBuilderCompatTest {
    public static final class FakeItemBuilder {
        public Object parentModel;
    }

    public static final class FakeModelGenerator {
        public ResourceLocation parent;
        public final Map<String, String> textures = new HashMap<>();

        public void parent(ResourceLocation value) {
            parent = value;
        }

        public void textures(Map<String, String> values) {
            textures.putAll(values);
        }
    }

    public static final class FakeBlockBuilder {
        public Object parentModel;
        public final Map<String, String> textures = new HashMap<>();
        public final FakeItemBuilder itemBuilder = new FakeItemBuilder();
        public Consumer<FakeModelGenerator> modelGenerator;
    }

    @Test
    void modelAliasSetsTheBlockAndItemParentModels() {
        FakeBlockBuilder builder = new FakeBlockBuilder();

        Object returned = KubeJSBlockBuilderCompat.model(builder, "minecraft:block/orientable");

        ResourceLocation expected = ResourceLocation.parse("minecraft:block/orientable");
        assertSame(builder, returned, "the legacy method remains chainable");
        assertEquals(expected, builder.parentModel);
        assertEquals(expected, builder.itemBuilder.parentModel);
        assertTrue(builder.modelGenerator != null,
                "custom block models must bypass KubeJS 7's extra tinted cube element");

        builder.textures.put("side", "alienevo:block/custom_side");
        FakeModelGenerator generated = new FakeModelGenerator();
        builder.modelGenerator.accept(generated);
        assertEquals(expected, generated.parent);
        assertEquals(Map.of("side", "alienevo:block/custom_side"), generated.textures);
    }

    @Test
    void emptyModelAliasRestoresTheGeneratedDefault() {
        FakeBlockBuilder builder = new FakeBlockBuilder();
        builder.parentModel = ResourceLocation.parse("minecraft:block/orientable");
        builder.itemBuilder.parentModel = builder.parentModel;

        KubeJSBlockBuilderCompat.model(builder, "");

        org.junit.jupiter.api.Assertions.assertNull(builder.parentModel);
        org.junit.jupiter.api.Assertions.assertNull(builder.itemBuilder.parentModel);
        org.junit.jupiter.api.Assertions.assertNull(builder.modelGenerator);
    }

    @Test
    void textureAllAliasSetsAllSixFacesAndParticleTexture() {
        Map<String, String> textures = new HashMap<>();

        KubeJSBlockBuilderCompat.applyTextureAll(textures, "alienevo:block/ice", false);

        for (String face : new String[] {"down", "up", "north", "south", "west", "east"}) {
            assertEquals("alienevo:block/ice", textures.get(face), face + " texture");
        }
        assertEquals("alienevo:block/ice", textures.get("particle"));
        org.junit.jupiter.api.Assertions.assertFalse(textures.containsKey("side"));
    }

    @Test
    void cardinalTextureAllSetsEveryOrientableModelTexture() {
        Map<String, String> textures = new HashMap<>();

        KubeJSBlockBuilderCompat.applyTextureAll(textures, "alienevo:block/galvan_pod", true);

        for (String key : new String[] {
                "down", "up", "north", "south", "west", "east",
                "side", "front", "top", "bottom", "particle"
        }) {
            assertEquals("alienevo:block/galvan_pod", textures.get(key), key + " texture");
        }
        assertTrue(textures.keySet().containsAll(
                java.util.Set.of("down", "up", "north", "south", "west", "east",
                        "side", "front", "top", "bottom", "particle")));
    }
}
