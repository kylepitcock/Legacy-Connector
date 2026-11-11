package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Regression tests for known AlienEvo script-scope identifier collisions. */
class KubeJSScriptScopeCompatTest {
    @Test
    void renamesOnlyTheAlienCodexComponentAliasAndItsReferences() {
        String original = """
                const Component = Java.loadClass("net.minecraft.network.chat.Component");

                global.getAlienDescription = function(id) {
                    return Component.translatable(`alienevo.alien_desc.${id}`);
                };
                """;
        byte[] input = original.getBytes(StandardCharsets.UTF_8);

        byte[] result = KubeJSScriptScopeCompat.rewriteKnownIdentifierCollisions(
                "alienevo", "kubejs_scripts/alien_codex.js", input);
        String rewritten = new String(result, StandardCharsets.UTF_8);

        assertEquals(2, count(rewritten, "AlienEvoComponent"));
        org.junit.jupiter.api.Assertions.assertFalse(rewritten.contains("const Component"));
        org.junit.jupiter.api.Assertions.assertTrue(rewritten.contains("AlienEvoComponent.translatable"));
    }

    @Test
    void renamesOnlyTheDisplayPlayerQuaternionAliasAndItsReference() {
        String original = """
                const InventoryScreen = Java.loadClass('net.minecraft.client.gui.screens.inventory.InventoryScreen');
                const Quaternionf = Java.loadClass('org.joml.Quaternionf');

                const rotation = new Quaternionf(1, 0, 0, 0);
                """;
        byte[] input = original.getBytes(StandardCharsets.UTF_8);

        byte[] result = KubeJSScriptScopeCompat.rewriteKnownIdentifierCollisions(
                "alienevo", "kubejs_scripts/display_player.js", input);
        String rewritten = new String(result, StandardCharsets.UTF_8);

        assertEquals(2, count(rewritten, "AlienEvoQuaternionf"));
        org.junit.jupiter.api.Assertions.assertFalse(rewritten.contains("const Quaternionf"));
        org.junit.jupiter.api.Assertions.assertTrue(rewritten.contains("new AlienEvoQuaternionf"));
    }

    @Test
    void leavesOtherNamespacesAndResourcesUnchanged() {
        byte[] input = "const Component = Java.loadClass('example.Component');".getBytes(StandardCharsets.UTF_8);

        assertSame(input, KubeJSScriptScopeCompat.rewriteKnownIdentifierCollisions(
                "afomni", "kubejs_scripts/alien_codex.js", input));
        assertSame(input, KubeJSScriptScopeCompat.rewriteKnownIdentifierCollisions(
                "alienevo", "kubejs_scripts/other.js", input));
    }

    @Test
    void leavesUnknownScriptRevisionsUnchanged() {
        byte[] input = """
                const Component = Java.loadClass("net.minecraft.network.chat.Component");
                const another = Component.translatable("example");
                const extra = Component.literal("not in the inspected script shape");
                """.getBytes(StandardCharsets.UTF_8);

        assertSame(input, KubeJSScriptScopeCompat.rewriteKnownIdentifierCollisions(
                "alienevo", "kubejs_scripts/alien_codex.js", input));
    }

    private static int count(String text, String value) {
        return text.split(java.util.regex.Pattern.quote(value), -1).length - 1;
    }
}
