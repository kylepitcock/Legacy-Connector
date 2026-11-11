package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Regression tests for the narrowly supported KubeJS 6 item-glow builder pattern. */
class KubeJSItemBuilderCompatTest {
    private static final Object GLINT_OVERRIDE = new Object();

    public static final class FakeItemBuilder {
        public Map<Object, Object> components;
    }

    public static final class FakeItemModifications {
        Object overriddenType;
        Object overriddenValue;

        public FakeItemModifications kjs$override(Object type, Object value) {
            overriddenType = type;
            overriddenValue = value;
            return this;
        }
    }

    @Test
    void glowTrueAddsTheMinecraftGlintOverrideComponent() {
        FakeItemBuilder builder = new FakeItemBuilder();

        assertSame(builder, KubeJSItemBuilderCompat.glow(builder, true, GLINT_OVERRIDE));

        assertEquals(Boolean.TRUE, builder.components.get(GLINT_OVERRIDE));
    }

    @Test
    void glowFalseClearsTheOverrideRatherThanForcingGlintOff() {
        FakeItemBuilder builder = new FakeItemBuilder();
        builder.components = new HashMap<>();

        KubeJSItemBuilderCompat.glow(builder, false, GLINT_OVERRIDE);

        assertEquals(true, builder.components.containsKey(GLINT_OVERRIDE));
        assertNull(builder.components.get(GLINT_OVERRIDE));
    }

    @Test
    void setItemBuilderCopiesOnlyTheVerifiedGlintComponent() {
        FakeItemBuilder builder = new FakeItemBuilder();
        FakeItemModifications modifications = new FakeItemModifications();
        KubeJSItemBuilderCompat.glow(builder, true, GLINT_OVERRIDE);

        KubeJSItemBuilderCompat.setItemBuilder(modifications, builder, GLINT_OVERRIDE);

        assertSame(GLINT_OVERRIDE, modifications.overriddenType);
        assertEquals(Boolean.TRUE, modifications.overriddenValue);
    }

    @Test
    void setItemBuilderRejectsUnreviewedBuilderComponents() {
        FakeItemBuilder builder = new FakeItemBuilder();
        builder.components = new HashMap<>();
        builder.components.put(GLINT_OVERRIDE, Boolean.TRUE);
        builder.components.put("unreviewed", Boolean.TRUE);

        assertThrows(UnsupportedOperationException.class,
                () -> KubeJSItemBuilderCompat.setItemBuilder(new FakeItemModifications(), builder, GLINT_OVERRIDE));
    }

    @Test
    void rewritesOnlyTheRecognizedKubeJs6GlowBuilderPattern() {
        String original = """
                const ItemBuilder = Java.loadClass("dev.latvian.mods.kubejs.item.custom.BasicItemJS$Builder");
                const builder = new ItemBuilder("alienevo:upgrade_crystal").glow(true);
                item.setItemBuilder(builder);
                """;
        byte[] input = original.getBytes(StandardCharsets.UTF_8);

        byte[] rewritten = KubeJSItemBuilderCompat.rewriteLegacyBuilderSource(input);
        String result = new String(rewritten, StandardCharsets.UTF_8);

        assertEquals(true, result.contains("dev.latvian.mods.kubejs.item.ItemBuilder"));
        assertEquals(false, result.contains("BasicItemJS$Builder"));
    }

    @Test
    void leavesOtherScriptUsesOfTheRemovedClassAlone() {
        String original = "Java.loadClass(\"dev.latvian.mods.kubejs.item.custom.BasicItemJS$Builder\");";
        byte[] input = original.getBytes(StandardCharsets.UTF_8);

        assertSame(input, KubeJSItemBuilderCompat.rewriteLegacyBuilderSource(input));
    }
}
